package com.bankapp.portal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "merchant_impersonations",
        indexes = {
                @Index(name = "idx_active_impersonations", columnList = "admin_username, active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantImpersonation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_username", nullable = false, updatable = false)
    private String adminUsername;

    @Column(name = "target_merchant_id", nullable = false, updatable = false)
    private String targetMerchantId;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;
}