package com.bankapp.portal.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    // PCI-DSS Practice: Pull secrets from env/vault at runtime. Never hardcode!
    private final String SECRET_KEY = "YourSuperSecretCryptoSecureSigningKeyForToPayLivePlatformEngine";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // Explicit 15-Minute Expiration Threshold in Milliseconds
    private final long EXPIRATION_TIME = 15 * 60 * 1000;

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setClaims(Map.of("role", role))
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}