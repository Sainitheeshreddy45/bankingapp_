package com.bankapp.portal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CookieAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private static final String COOKIE_NAME = "AUTH_TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String token = null;

        // 1. Extract the target token from the HTTP Cookie Array safely
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. ✅ FIXED: If the cookie is missing completely, DO NOT short-circuit!
        // Instead, let the request flow naturally down to standard Spring Security.
        // Spring's HttpSecurity config will handle allowing public paths while blocking protected paths.
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Cryptographic Signature & Expiration Check
            if (tokenProvider.isTokenValid(token)) {
                String email = tokenProvider.extractUsername(token);
                List<SimpleGrantedAuthority> authorities = tokenProvider.extractAuthorities(token);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Authorize thread execution path
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                // ✅ FIXED: Token exists but failed cryptographic or timeline validations!
                SecurityContextHolder.clearContext();
                triggerSessionExpiredResponse(response);
                return;
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 4. Token is structurally malformed, corrupted, or explicitly expired
            SecurityContextHolder.clearContext();
            triggerSessionExpiredResponse(response);
        }
    }

    /**
     * 🛡️ Emits the precise structured JSON your frontend interceptor needs to clear memory spaces
     */
    private void triggerSessionExpiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorPayload = Map.of(
                "message", "Your structural credentials have expired or are missing.",
                "errorCode", "SESSION_EXPIRED"
        );

        new ObjectMapper().writeValue(response.getOutputStream(), errorPayload);
    }
}