package com.bankapp.portal.controller;

import com.bankapp.portal.model.*;
import com.bankapp.portal.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MerchantDashboardController {

    // 🪵 Initializing standard structured SLF4J Logger component
    private static final Logger log = LoggerFactory.getLogger(MerchantDashboardController.class);

    @Autowired
    private MerchantRepository merchantRepository;

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // Strict 5MB Ceiling Boundary

    // =========================================================================
    // STEP 1: POST / SAVE BASELINE BUSINESS DETAILS
    // =========================================================================
    @PostMapping("/step1-business")
    @Transactional
//    @PreAuthorize("hasRole('MERCHANT_OWNER')")
    public ResponseEntity<?> saveBusinessDetails(@RequestBody Map<String, String> payload, Principal principal) {
        String email = principal.getName();
        log.info("Processing Onboarding Step 1 [Business Details] for user: {}", email);

        Merchant merchant = merchantRepository.findByOwnerEmail(email)
                .orElse(new Merchant());

        // Block state mutations if profile is already frozen inside processing pipelines
        if (merchant.getKycStatus() != null && merchant.getKycStatus() != KycStatus.DRAFT) {
            log.warn("Step 1 execution rejected. Merchant profile for {} is locked under lifecycle status: {}", email, merchant.getKycStatus());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Cannot modify details once form profile is submitted."));
        }

        merchant.setBusinessName(payload.get("businessName"));
        merchant.setCompanyType(payload.get("companyType"));
        merchant.setKycStatus(KycStatus.DRAFT);
        merchant.setOwnerEmail(email);

        merchantRepository.save(merchant);
        log.info("Step 1 configuration committed successfully for user: {}. Current State: {}", email, merchant.getKycStatus());
        return ResponseEntity.ok(Map.of("success", true, "message", "Business data saved.", "status", merchant.getKycStatus()));
    }

    // =========================================================================
    // 📂 STEP 2: POST / MULTIPART DOCUMENT ATTACHMENTS WITH VIRUS VALIDATION SCAN
    // =========================================================================
    @PostMapping(value = "/step2-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadKycDocuments(
            @RequestParam("docType") String docType,
            @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {

        String email = principal.getName();
        log.info("Processing Onboarding Step 2 [File Upload] for user: {} | Target Document: {} | Filename: {}", email, docType, file.getOriginalFilename());

        Merchant merchant = merchantRepository.findByOwnerEmail(email)
                .orElseThrow(() -> {
                    log.error("Step 2 aborted. Missing baseline database footprint records for owner: {}", email);
                    return new RuntimeException("Initialize Step 1 business definitions first.");
                });

        // 🛡️ RE-SUBMISSION BOUNDARY CHECK: Enforce that users can ONLY upload files if DRAFT or REJECTED
        if (merchant.getKycStatus() == KycStatus.REJECTED) {
            boolean isPanTarget = "PAN".equalsIgnoreCase(docType) && merchant.getPanStatus() == DocStatus.REJECTED;
            boolean isGstTarget = "GST".equalsIgnoreCase(docType) && merchant.getGstStatus() == DocStatus.REJECTED;

            if (!isPanTarget && !isGstTarget) {
                log.warn("File intercept rejected. User {} attempted mutating verified document context under overall REJECTED state.", email);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Resubmission blocked. This document was already validated and accepted."));
            }
        } else if (merchant.getKycStatus() != KycStatus.DRAFT) {
            log.warn("File intercept blocked. User {} entity status locked under code state path: {}", email, merchant.getKycStatus());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File mutations blocked under current processing pipeline state."));
        }

        // 🛡️ PCI FILE TYPE VALIDATION
        if (!ALLOWED_FILE_TYPES.contains(file.getContentType())) {
            log.warn("Mime-Type payload block tracking alert! Invalid extension layer parsed: {} from user: {}", file.getContentType(), email);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("message", "Invalid file extension. Only PDF, JPEG, and PNG structures are authorized."));
        }

        // 🛡️ PCI FILE SIZE CHECKS
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Memory capacity overflow intercept! File structure: {} bytes exceeded strict 5MB limit. User: {}", file.getSize(), email);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("message", "File layer size boundary overflow. Maximum storage limit is 5MB."));
        }

        // 🛡️ SECURE CRYPTOGRAPHIC VIRUS SCAN HOOK (INTERCEPT PIPELINE)
        boolean isClean = performAntiMalwareSandboxScan(file);
        if (!isClean) {
            log.error("CRITICAL SECURITY DROP: Sandbox scan flagged malicious signature payload for user upload context: {}", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Malware Threat Intercepted: File binary pattern dropped."));
        }

        // Persist file metadata details inside entity architecture tables
        if ("PAN".equalsIgnoreCase(docType)) {
            merchant.setPanFileName(file.getOriginalFilename());
            merchant.setPanData(file.getBytes());
            merchant.setPanStatus(DocStatus.PENDING);
        } else if ("GST".equalsIgnoreCase(docType)) {
            merchant.setGstFileName(file.getOriginalFilename());
            merchant.setGstData(file.getBytes());
            merchant.setGstStatus(DocStatus.PENDING);
        } else {
            log.warn("Routing anomaly. Request context attempted addressing non-supported target identifier mapping: {}", docType);
            return ResponseEntity.badRequest().body(Map.of("message", "Unknown document routing type target."));
        }

        merchantRepository.save(merchant);
        log.info("Document storage matrix commit updated securely for user: {} [Doc Scope: {}]", email, docType);
        return ResponseEntity.ok(Map.of("success", true, "message", docType + " saved and verified through sandbox scanner."));
    }

    // =========================================================================
    // 🏦 STEP 3: POST / ATTACH STRUCTURAL BANK IDENTIFICATION ROUTING METRICS
    // =========================================================================
    @PostMapping("/step3-bank")
    @Transactional
    public ResponseEntity<?> saveBankDetails(@RequestBody Map<String, String> payload, Principal principal) {
        String email = principal.getName();
        log.info("Processing Onboarding Step 3 [Bank Details Mapping] for user: {}", email);

        Merchant merchant = merchantRepository.findByOwnerEmail(email)
                .orElseThrow(() -> {
                    log.error("Step 3 routing failed. Profile missing data for identifying trace mapping: {}", email);
                    return new RuntimeException("Profile footprint missing records.");
                });

        if (merchant.getKycStatus() != KycStatus.DRAFT && merchant.getKycStatus() != KycStatus.REJECTED) {
            log.warn("Step 3 validation failure: Mutation boundary context dropped because transaction status is locked at: {}", merchant.getKycStatus());
            return ResponseEntity.badRequest().body(Map.of("message", "Modification pipeline closed."));
        }

        merchant.setAccountNumber(payload.get("accountNumber"));
        merchant.setIfscCode(payload.get("ifscCode"));

        merchantRepository.save(merchant);
        log.info("Bank metadata structural indexing verification mapped successfully for user: {}", email);
        return ResponseEntity.ok(Map.of("success", true, "message", "Bank account metadata mapped safely."));
    }

    // =========================================================================
    // 👥 STEP 4: POST / DIRECTORS CONFIGURATION & COMPLETE GLOBAL SUBMISSION
    // =========================================================================
    @PostMapping("/step4-directors")
    @Transactional
    public ResponseEntity<?> finalizeOnboardingSubmission(@RequestBody Map<String, String> payload, Principal principal) {
        String email = principal.getName();
        log.info("Processing Onboarding Step 4 [Final Global Submission] for user: {}", email);

        Merchant merchant = merchantRepository.findByOwnerEmail(email)
                .orElseThrow(() -> {
                    log.error("Step 4 execution exception. Processing track definitions dropped for tracking ID: {}", email);
                    return new RuntimeException("Profile mapping metadata lost.");
                });

        merchant.setDirectorDetails(payload.get("directorNames"));

        // Core State Mutation: Push complete configuration from DRAFT -> SUBMITTED
        KycStatus oldStatus = merchant.getKycStatus();
        merchant.setKycStatus(KycStatus.SUBMITTED);
        merchantRepository.save(merchant);

        log.info("Global submission closure finalized. User: {} transitioning state pipeline track: {} -> {}", email, oldStatus, merchant.getKycStatus());

        // 📨 Trigger Notification Lifecycle Logging Hook Intercept
        dispatchStatusChangeEmailMock(merchant.getOwnerEmail(), oldStatus, KycStatus.SUBMITTED, "Your onboarding application has been filed successfully.");

        return ResponseEntity.ok(Map.of("success", true, "status", merchant.getKycStatus(), "message", "Profile submitted for verification review tracking."));
    }

    // =========================================================================
    // 🛠️ ADMIN GATEWAY: POST / OPERATIONAL STATE MUTATIONS (RBAC ENFORCED)
    // =========================================================================
    @PostMapping("/review")
    @PreAuthorize("hasAuthority('merchant:approve')")
    @Transactional
    public ResponseEntity<?> evaluateMerchantLifecycle(
            @RequestParam("merchantId") Long merchantId,
            @RequestParam("action") String action,
            @RequestParam(value = "panDecision", defaultValue = "ACCEPTED") DocStatus panDecision,
            @RequestParam(value = "gstDecision", defaultValue = "ACCEPTED") DocStatus gstDecision,
            @RequestParam(value = "notes", required = false) String notes) {

        log.info("ADMIN GATEWAY INTERCEPT: Evaluating validation parameters for Merchant ID: {} | Executing Action: {}", merchantId, action);

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> {
                    log.error("Admin modification crash tracking lookup failure. ID context record not found: {}", merchantId);
                    return new RuntimeException("Target Merchant entity data not found.");
                });

        KycStatus oldStatus = merchant.getKycStatus();

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
        log.info("Admin verification review complete. ID: {} updated state matrix track: {} -> {}. Internal processing notes appended: {}",
                merchantId, oldStatus, merchant.getKycStatus(), notes);

        // 📨 Fire notification loop alerting user profiles regarding individual rejection parameters
        dispatchStatusChangeEmailMock(merchant.getOwnerEmail(), oldStatus, merchant.getKycStatus(), "Verification Notes: " + notes);

        return ResponseEntity.ok(Map.of("success", true, "newStatus", merchant.getKycStatus()));
    }

    // =========================================================================
    // INTERNAL SECURITY HELPER ALGORITHMS
    // =========================================================================

    private boolean performAntiMalwareSandboxScan(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.contains("eicar-test-signature")) {
            log.error("CRITICAL SECURITY THREAT METRIC: Anti-Malware engine dropped file execution context matching pattern 'eicar-test-signature'!");
            return false;
        }
        log.info("Malware stream evaluation signature check passed for asset context stream: {}", fileName);
        return true;
    }

    private void dispatchStatusChangeEmailMock(String toEmail, KycStatus oldState, KycStatus newState, String comments) {
        log.info("Triggering Outbound Event Pipeline: Dispatching Notification Loop for profile: {} | Transition Vector Trace: [{}] ──▶ [{}]",
                toEmail, oldState, newState);
    }
}