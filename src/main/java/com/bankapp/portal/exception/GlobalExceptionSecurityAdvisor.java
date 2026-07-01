package com.bankapp.portal.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionSecurityAdvisor {

    /**
     * Explicit Security Requirement: Catches method-level authentication failures
     * and strictly returns a 403 Forbidden status, avoiding 404 resource leakage.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("SECURITY WARNING: Resource authorization boundary breach blocked.");

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "message", "Access Denied: Your assigned token lacks permissions for this resource context.",
                        "errorCode", "INSUFFICIENT_PRIVILEGES"
                ));
    }

    /**
     * Prevents internal server stack traces from being exposed in public response bodies.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericFallback(Exception ex) {
        log.error("CRITICAL CRASH CAUGHT AT SYSTEM BOUNDARY: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "message", "An unexpected runtime error occurred processing your transaction.",
                        "errorCode", "INTERNAL_SERVER_ERROR"
                ));
    }
}