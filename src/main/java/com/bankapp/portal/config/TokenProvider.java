package com.bankapp.portal.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

    private final SecretKey signingKey;
    private static final long ACCESS_TOKEN_VALIDITY_MS = 15 * 60 * 1000;

    // Secure extraction out of properties management contexts
    public TokenProvider(@Value("${app.jwt.secret:Py+iM0RLDoelxAJ3SZxL5L1z7jujb2WFzT6WfLp08AY=hIltiHm2ugcSpIKmNS+pjV}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * UPDATED: Now maps both Role groups and granular Permissions into separate claims arrays.
     */
    public String generateToken(String email, List<String> roles, List<String> permissions) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("roles", roles)            // 👈 E.g., ["ROLE_SUPER_ADMIN", "ROLE_OPS_ADMIN"]
                .claim("permissions", permissions) // 👈 E.g., ["merchant:approve", "txn:read"]
                .issuedAt(new Date(now))
                .expiration(new Date(now + ACCESS_TOKEN_VALIDITY_MS))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * UPDATED: Merges both Roles and Permissions into a single flat GrantedAuthority context.
     * This provides support for both hasRole() and hasAuthority() out-of-the-box in stateless contexts.
     */
    @SuppressWarnings("unchecked")
    public List<SimpleGrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 1. Extract and map structural Roles (Satisfies hasRole / hasAnyRole checks)
        List<String> roles = claims.get("roles", List.class);
        if (roles != null) {
            roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // 2. Extract and map action Permissions (Satisfies hasAuthority / hasAnyAuthority checks)
        List<String> permissions = claims.get("permissions", List.class);
        if (permissions != null) {
            permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}