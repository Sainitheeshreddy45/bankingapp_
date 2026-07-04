package com.bankapp.portal.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    private String id;

    // 👑 Ensure the field variable name matches your query text exactly
    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "payment_mode", nullable = false)
    private String paymentMode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}