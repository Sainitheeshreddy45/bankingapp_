package com.bankapp.portal.repository;

import com.bankapp.portal.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    // Exposes native read-only mapping extensions. No update hooks declared.
}