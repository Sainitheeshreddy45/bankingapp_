package com.bankapp.portal.controller;

import com.bankapp.portal.model.AdminAuditLog;
import com.bankapp.portal.model.DocStatus;
import com.bankapp.portal.model.KycStatus;
import com.bankapp.portal.model.Merchant;
import com.bankapp.portal.service.AdminPortalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/management")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class AdminPortalController {

    private final AdminPortalService adminManagementService;

    @GetMapping("/merchants/queue")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'OPS_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_OPS_ADMIN', 'merchant:approve')")
    public ResponseEntity<List<Merchant>> getApprovalQueue() {
        log.info("HTTP Request: Fetching active evaluation registration queue...");
        List<Merchant> queue = adminManagementService.getSubmittedApprovalQueue();
        return ResponseEntity.ok(queue);
    }

    @PostMapping("/merchants/{merchantId}/review")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'OPS_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_OPS_ADMIN', 'merchant:approve')")
    public ResponseEntity<?> evaluateMerchantLifecycle(
            @PathVariable Long merchantId,
            @RequestParam("action") String action,
            @RequestParam(value = "panDecision", defaultValue = "ACCEPTED") DocStatus panDecision,
            @RequestParam(value = "gstDecision", defaultValue = "ACCEPTED") DocStatus gstDecision,
            @RequestParam(value = "notes", required = false) String notes,
            HttpServletRequest request) {

        log.info("HTTP Request: Submitting administrative evaluation details for Merchant: {}", merchantId);
        String operatorEmail = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "ops_manager@bank.com";

        KycStatus newStatus = adminManagementService.evaluateMerchantLifecycle(
                merchantId, action, panDecision, gstDecision, notes, operatorEmail, request.getRemoteAddr()
        );

        return ResponseEntity.ok(Map.of("success", true, "newStatus", newStatus));
    }

    @GetMapping("/merchants/{merchantId}/diff-view")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'OPS_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_OPS_ADMIN')")
    public ResponseEntity<?> getMerchantRevisionDiff(@PathVariable Long merchantId) {
        log.info("HTTP Request: Loading audit structural properties comparison for ID: {}", merchantId);
        Map<String, Object> diffData = adminManagementService.getMerchantRevisionDiff(merchantId);
        return ResponseEntity.ok(diffData);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<AdminAuditLog>> getAuditLogs() {
        log.info("HTTP Request: Accessing system comprehensive administrative security logs...");
        return ResponseEntity.ok(adminManagementService.getAllAuditLogs());
    }

    @PostMapping("/merchants/{merchantId}/action")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'OPS_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_OPS_ADMIN')")
    public ResponseEntity<?> executeOperationalUtility(
            @PathVariable Long merchantId,
            @RequestParam("command") String command,
            HttpServletRequest request) {

        log.info("HTTP Request: Dispatching strategic infrastructure control execution [{}] to Merchant target ID: {}", command, merchantId);
        String operatorEmail = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "ops_manager@bank.com";

        KycStatus status = adminManagementService.executeOperationalUtility(
                merchantId, command, operatorEmail, request.getRemoteAddr()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "commandExecuted", command,
                "currentStatus", status
        ));
    }
}