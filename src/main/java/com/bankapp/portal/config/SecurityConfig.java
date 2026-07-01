package com.bankapp.portal.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieAuthenticationFilter cookieFilter) throws Exception {

        XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/verify-2fa",
                                "/api/v1/auth/logout",
                                "/api/v1/onboarding/**",
                                "/api/v1/admin/**" // 👈 FIXED: Prevents CSRF from blocking admin POST reviews and action tools
                        )
                )
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .frameOptions(frame -> frame.deny())
                )
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Open Public Endpoints
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/verify-2fa",
                                "/actuator/health"
                        ).permitAll()

                        // 🔒 Authenticated Session Discovery Check
                        .requestMatchers("/api/v1/auth/me").authenticated()

                        // 🔒 DELEGATED TO METHOD SECURITY
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        .requestMatchers("/api/v1/onboarding/**").authenticated()

                        .anyRequest().authenticated()
                )
                // ⚙️ Core Filter Architecture Sequence
                .addFilterBefore(cookieFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter(), CookieAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), CookieAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Xsrf-Token", "Idempotency-Key"));
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "X-Xsrf-Token"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public Filter rateLimitingFilter() {
        return new Filter() {
            private final Map<String, CopyOnWriteArrayList<LocalDateTime>> attemptCache = new ConcurrentHashMap<>();

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                if (httpRequest.getRequestURI().contains("/api/v1/auth/login") && "POST".equalsIgnoreCase(httpRequest.getMethod())) {
                    String ipKey = httpRequest.getRemoteAddr();
                    LocalDateTime now = LocalDateTime.now();

                    attemptCache.computeIfAbsent(ipKey, k -> new CopyOnWriteArrayList<>());
                    CopyOnWriteArrayList<LocalDateTime> attempts = attemptCache.get(ipKey);
                    attempts.removeIf(time -> time.isBefore(now.minusMinutes(15)));

                    if (attempts.size() >= 75) {
                        httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        httpResponse.setContentType("application/json");
                        httpResponse.getWriter().write("{\"message\": \"Too many login attempts. Access locked for 15 minutes.\"}");
                        return;
                    }
                    attempts.add(now);
                }
                chain.doFilter(request, response);
            }
        };
    }

    private static class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}