package com.bankapp.portal.service;

import com.bankapp.portal.model.*;
import com.bankapp.portal.repository.AdminAuditLogRepository;
import com.bankapp.portal.repository.ImpersonationRepository;
import com.bankapp.portal.repository.MerchantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPortalService {

    private final MerchantRepository merchantRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final ImpersonationRepository impersonationRepository;
    private final ObjectMapper mapper;

    public List<Merchant> getSubmittedApprovalQueue() {
        log.info("Querying repository layer for submitted KYC verification profiles...");
        return merchantRepository.findAll().stream()
                .filter(m -> m.getKycStatus() == KycStatus.SUBMITTED)
                .toList();
    }

    public List<AdminAuditLog> getAllAuditLogs() {
        log.info("Loading system admin audit trails...");
        return adminAuditLogRepository.findAll();
    }

    public Map<String, Object> getMerchantRevisionDiff(Long merchantId) {
        log.info("Computing structural revision snapshot maps for entity reference: {}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant footprint data missing."));

        return Map.of(
                "merchantId", merchantId,
                "fieldModified", "Settlement Routing (IFSC & Account)",
                "previousValue", "No active distribution destination registered (DRAFT)",
                "submittedValue", String.format("Acc: %s | IFSC: %s",
                        merchant.getAccountNumber() != null ? merchant.getAccountNumber() : "Not Provided",
                        merchant.getIfscCode() != null ? merchant.getIfscCode() : "Not Provided")
        );
    }

    // =========================================================================
    // 🛠️ MERCHANT LIFECYCLE & MUTATION OPERATORS
    // =========================================================================

    public KycStatus evaluateMerchantLifecycle(Long merchantId, String action, DocStatus panDecision,
                                               DocStatus gstDecision, String notes, String operatorEmail, String clientIp) {
        log.info("Processing baseline structural review lifecycle mutations for ID: {}", merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not located."));

        String preStateJson;
        try {
            preStateJson = mapper.writeValueAsString(merchant);
        } catch (Exception ex) {
            preStateJson = String.format("{\"kycStatus\":\"%s\"}", merchant.getKycStatus());
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            merchant.setKycStatus(KycStatus.APPROVED);
            merchant.setPanStatus(DocStatus.ACCEPTED);
            merchant.setGstStatus(DocStatus.ACCEPTED);
        } else {
            merchant.setKycStatus(KycStatus.REJECTED);
            merchant.setPanStatus(panDecision);
            merchant.setGstStatus(gstDecision);
        }

        merchantRepository.save(merchant);

        String postStateJson;
        try {
            postStateJson = mapper.writeValueAsString(merchant);
        } catch (Exception ex) {
            postStateJson = String.format("{\"kycStatus\":\"%s\", \"notes\":\"%s\"}", merchant.getKycStatus(), notes);
        }

        AdminAuditLog auditEntry = new AdminAuditLog(
                operatorEmail,
                "MERCHANT_" + action.toUpperCase(),
                clientIp,
                preStateJson,
                postStateJson
        );
        adminAuditLogRepository.save(auditEntry);

        return merchant.getKycStatus();
    }

    @PreAuthorize("hasAuthority('merchant:approve')")
    public void processMerchantKycQueue(String executionAdmin, String clientIp, ApprovalActionRequest request, Merchant targetMerchantEntity) {
        log.info("Processing specialized payload Kyc Queue updates for Merchant entity reference: {}", targetMerchantEntity.getId());
        try {
            String preStateJson = mapper.writeValueAsString(targetMerchantEntity);

            targetMerchantEntity.setKycStatus(KycStatus.valueOf(request.getStatus().toUpperCase()));
            merchantRepository.save(targetMerchantEntity);

            String postStateJson = mapper.writeValueAsString(targetMerchantEntity);

            AdminAuditLog logEntry = new AdminAuditLog(
                    executionAdmin,
                    "MERCHANT_STATUS_MUTATION_" + request.getStatus(),
                    clientIp,
                    preStateJson,
                    postStateJson
            );
            adminAuditLogRepository.save(logEntry);

        } catch (Exception ex) {
            throw new RuntimeException("Audit tracking serialization execution flow failed.", ex);
        }
    }

    public KycStatus executeOperationalUtility(Long merchantId, String command, String operatorEmail, String clientIp) {
        log.info("Dispatching utility core routine [{}] on target identity ID: {}", command, merchantId);
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant record not located."));

        String preStateJson;
        try {
            preStateJson = mapper.writeValueAsString(merchant);
        } catch (Exception ex) {
            preStateJson = String.format("{\"actionCommand\":\"NONE\", \"kycStatus\":\"%s\"}", merchant.getKycStatus());
        }

        switch (command.toUpperCase()) {
            case "BLOCK" -> {
                merchant.setKycStatus(KycStatus.REJECTED);
                log.info("Merchant reference ID {} has been suspended.", merchantId);
            }
            case "UNBLOCK" -> {
                merchant.setKycStatus(KycStatus.APPROVED);
                log.info("Merchant reference ID {} has been unblocked.", merchantId);
            }
            case "TRIGGER_SETTLEMENT" -> {
                log.info("Settlement dispatch successfully initialized for target account: {}", merchant.getAccountNumber());
            }
            case "RESET_2FA" -> {
                log.info("MFA authentication state invalidation token assigned to identity: {}", merchant.getOwnerEmail());
            }
            default -> throw new IllegalArgumentException("Unsupported operational control boarding action command: " + command);
        }

        merchantRepository.save(merchant);

        String postStateJson;
        try {
            postStateJson = mapper.writeValueAsString(merchant);
        } catch (Exception ex) {
            postStateJson = String.format("{\"actionCommand\":\"%s\", \"kycStatus\":\"%s\"}", command, merchant.getKycStatus());
        }

        AdminAuditLog auditEntry = new AdminAuditLog(
                operatorEmail,
                "UTILITY_" + command.toUpperCase(),
                clientIp,
                preStateJson,
                postStateJson
        );
        adminAuditLogRepository.save(auditEntry);

        return merchant.getKycStatus();
    }

    // =========================================================================
    // 🛡️ SECURITY PROXIES & ACCREDITATION MECHANICS
    // =========================================================================

    @PreAuthorize("hasAuthority('merchant:impersonate')")
    public String initiateImpersonationProxy(String currentAdmin, ImpersonationRequest context) {
        log.info("Security Context Intervention: Initializing admin proxy credentials session for user: {}", currentAdmin);

        impersonationRepository.findByAdminUsernameAndActiveTrue(currentAdmin)
                .ifPresent(session -> {
                    session.setActive(false);
                    impersonationRepository.save(session);
                });

        MerchantImpersonation secureProxy = MerchantImpersonation.builder()
                .adminUsername(currentAdmin)
                .targetMerchantId(context.getTargetMerchantId())
                .reason(context.getReason())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        impersonationRepository.save(secureProxy);

        adminAuditLogRepository.save(new AdminAuditLog(
                currentAdmin,
                "IMPERSONATION_INITIATED",
                "0.0.0.0",
                null,
                "Target Merchant: " + context.getTargetMerchantId()
        ));

        return "Impersonation session established. Session expires automatically in 15 minutes.";
    }
}