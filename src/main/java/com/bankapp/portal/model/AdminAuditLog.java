package com.bankapp.portal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_username", nullable = false, updatable = false)
    private String adminUsername;

    @Column(name = "action", nullable = false, updatable = false)
    private String action;

    @Column(name = "ip_address", nullable = false, updatable = false)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Stores the pre-mutation state snapshot as an immutable TEXT payload block
    @Column(name = "pre_state_json", columnDefinition = "TEXT", updatable = false)
    private String preStateJson;

    // Stores the post-mutation state snapshot as an immutable TEXT payload block
    @Column(name = "post_state_json", columnDefinition = "TEXT", updatable = false)
    private String postStateJson;

    // Required arguments constructor explicitly matched to your AdminOpsService signatures
    public AdminAuditLog(String adminUsername, String action, String ipAddress, String preStateJson, String postStateJson) {
        this.adminUsername = adminUsername;
        this.action = action;
        this.ipAddress = ipAddress;
        this.preStateJson = preStateJson;
        this.postStateJson = postStateJson;
        this.timestamp = LocalDateTime.now();
    }
}