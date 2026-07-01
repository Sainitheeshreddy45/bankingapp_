package com.bankapp.portal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotent_requests",
        indexes = {
                @Index(name = "idx_idempotency_lookup", columnList = "idempotency_key, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotentRequest {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // IN_PROGRESS, COMPLETED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Custom constructor to perfectly match your service instantiation signatures
    public IdempotentRequest(String idempotencyKey, int code, String responseBody, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.responseBody = responseBody;
        this.status = "COMPLETED"; // Instantiating directly maps to completed states in your fallback code
        this.requestPath = "/api/v1/merchant/dashboard/transactions/refund"; // Fallback default context path
        this.createdAt = createdAt;
    }
}