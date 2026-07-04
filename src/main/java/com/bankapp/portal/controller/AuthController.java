package com.bankapp.portal.controller;

import com.bankapp.portal.config.TokenProvider;
import com.bankapp.portal.dto.AuthRequests.*;
import com.bankapp.portal.dto.UserSessionResponse;
import com.bankapp.portal.model.Role;
import com.bankapp.portal.model.User;
import com.bankapp.portal.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    private static final String COOKIE_NAME = "AUTH_TOKEN";
    private static final String COOKIE_PATH = "/";
    private static final String SAMESITE_STRICT = "Strict";
    private static final int TOKEN_VALIDITY_SECONDS = 15 * 60;
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String DEFAULT_FALLBACK_ROLE = "ROLE_MERCHANT_OWNER";

    // =========================
    // SIGNUP
    // =========================
    @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, String>> registerAccount(@Valid @RequestBody SignUpRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email already exists"));
        }

        User user = new User();
        user.setEmail(request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(encodedPassword);
        user.getPasswordHistory().add(encodedPassword);
        user.setPasswordUpdatedAt(LocalDateTime.now());

        Role role = userRepository.findRoleByName(request.getTargetRole())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        user.setRoles(Collections.singleton(role));
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created successfully"));
    }

    // =========================
    // LOGIN (FIXED: MAPS BOTH ROLES & PERMISSIONS)
    // =========================
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticateSession(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        // SECURED: Log the email instead of the raw plaintext password
        log.info("Login process started for user email: {}", request.getEmail());

        var userOpt = userRepository.findByEmailWithPermissions(request.getEmail());

        // FIXED: Clean validation logic that prevents the short-circuit fall-through bug
        if (userOpt.isEmpty()) {
            log.warn("Login failed: User email not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        User user = userOpt.get(); // Safe to call now because we verified it's not empty

        // Validate the password against the retrieved hash
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!passwordMatches) {
            log.warn("Login failed: Incorrect password for user: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        // 1. Extract structural Roles list (e.g., ["ROLE_SUPER_ADMIN"])
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // 2. Extract fine-grained Permissions list (e.g., ["merchant:approve"])
        List<String> permissions = user.getRoles().stream()
                .filter(r -> r.getPermissions() != null)
                .flatMap(r -> r.getPermissions().stream())
                .collect(Collectors.toList());

        // 3. Invoke updated TokenProvider constructor mapping BOTH sets
        String token = tokenProvider.generateToken(user.getEmail(), roles, permissions);

        if (response != null) {
            response.addCookie(createSecureSessionCookie(token, TOKEN_VALIDITY_SECONDS));
        }

        String targetRole = roles.stream()
                .findFirst()
                .orElse(DEFAULT_FALLBACK_ROLE);

        return ResponseEntity.ok(Map.of(
                "user", Map.of(
                        "email", user.getEmail(),
                        "role", targetRole
                ),
                "message", "Credentials verified successfully."
        ));
    }
    // =========================
    // LOGOUT (FULLY SYSTEM RESET)
    // =========================
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        response.addCookie(createSecureSessionCookie(null, 0));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        jakarta.servlet.http.Cookie csrfCookie = new jakarta.servlet.http.Cookie("XSRF-TOKEN", null);
        csrfCookie.setHttpOnly(false);
        csrfCookie.setSecure(true);
        csrfCookie.setPath("/");
        csrfCookie.setMaxAge(0);
        response.addCookie(csrfCookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully across all layers."));
    }

    // =========================
    // ME ENDPOINT
    // =========================
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserSessionResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = getEmail(authentication);

        User user = userRepository.findByEmailWithPermissions(email)
                .orElseThrow(() -> new NoSuchElementException("User context could not be re-evaluated."));

        String dbRole = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse(DEFAULT_FALLBACK_ROLE);

        String finalRole = dbRole.startsWith(ROLE_PREFIX) ? dbRole : ROLE_PREFIX + dbRole;

        Set<String> permissions = user.getRoles().stream()
                .filter(r -> r.getPermissions() != null)
                .flatMap(r -> r.getPermissions().stream())
                .collect(Collectors.toSet());

        UserSessionResponse.UserPayload payload = new UserSessionResponse.UserPayload(
                email,
                finalRole,
                permissions,
                "APPROVED", "ToPay Corp", "PRIVATE_LIMITED", "XXXX1234", "HDFC0000123", "Jane Doe", "ACCEPTED", "ACCEPTED"
        );

        return ResponseEntity.ok(new UserSessionResponse(payload, false));
    }

    private Cookie createSecureSessionCookie(String token, int maxAge) {
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", SAMESITE_STRICT);
        return cookie;
    }

    private String getEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails u) return u.getUsername();
        return authentication.getName();
    }
}