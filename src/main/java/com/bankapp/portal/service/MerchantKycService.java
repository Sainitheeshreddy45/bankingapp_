package com.bankapp.portal.service;

import com.bankapp.portal.model.KycStatus;
import com.bankapp.portal.model.Merchant;
import com.bankapp.portal.model.MerchantDocument;
import com.bankapp.portal.model.OnboardingRequest;
import com.bankapp.portal.repository.MerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MerchantKycService {

    private final AntiVirusScanner avScanner;
    private final MerchantRepository merchantRepository;

    public MerchantKycService(AntiVirusScanner avScanner, MerchantRepository merchantRepository) {
        this.avScanner = avScanner;
        this.merchantRepository = merchantRepository;
    }

    private static final List<String> AUTHORIZED_MIME_TYPES = Arrays.asList("application/pdf", "image/jpeg", "image/png");

    @Transactional
    public Merchant createDraftMerchant(String email, OnboardingRequest dto) {
        // Enforce DB guard check using the correct field mapping pattern
        merchantRepository.findByOwnerEmail(email).ifPresent(m -> {
            throw new IllegalStateException("A verification profile already exists matching this identity pointer.");
        });

        Merchant merchant = new Merchant();
        merchant.setOwnerEmail(email);
        merchant.setBusinessName(dto.getBusinessName());
        merchant.setAccountNumber(dto.getBankAccountNumber());
        merchant.setIfscCode(dto.getBankIfsc().toUpperCase());
        merchant.setKycStatus(KycStatus.DRAFT);

        return merchantRepository.save(merchant);
    }

    @Transactional
    public void uploadKycDocument(Merchant merchant, String docType, MultipartFile file) throws IOException {
        if (!AUTHORIZED_MIME_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported file type. Authorized formats: PDF, JPEG, PNG.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size boundaries exceeded. Maximum ceiling is 5MB.");
        }
        if (!avScanner.isClean(file)) {
            throw new SecurityException("Malicious signature blueprint caught during validation scan.");
        }

        // Targeted Document-Level Multi-Step Re-submission execution checks
        boolean existingDocTypePresent = merchant.getDocuments().stream()
                .anyMatch(d -> d.getDocumentType().equalsIgnoreCase(docType));

        if (existingDocTypePresent) {
            MerchantDocument doc = merchant.getDocuments().stream()
                    .filter(d -> d.getDocumentType().equalsIgnoreCase(docType))
                    .findFirst()
                    .orElseThrow();

            if (!"REJECTED".equalsIgnoreCase(doc.getStatus()) && merchant.getKycStatus() != KycStatus.DRAFT) {
                throw new IllegalStateException("Cannot replace document unless layout is DRAFT or individual item is REJECTED.");
            }

            doc.setFilePath("/storage/uploads/" + file.getOriginalFilename());
            doc.setStatus("PENDING");
            doc.setUploadedAt(LocalDateTime.now());
        } else {
            MerchantDocument newDoc = new MerchantDocument();
            newDoc.setMerchant(merchant);
            newDoc.setDocumentType(docType);
            newDoc.setFilePath("/storage/uploads/" + file.getOriginalFilename());
            newDoc.setStatus("PENDING");
            newDoc.setUploadedAt(LocalDateTime.now());
            merchant.getDocuments().add(newDoc);
        }

        // Write file raw bytes directly onto your model storage targets as well
        if ("PAN".equalsIgnoreCase(docType)) {
            merchant.setPanFileName(file.getOriginalFilename());
            merchant.setPanData(file.getBytes());
        } else if ("GST".equalsIgnoreCase(docType)) {
            merchant.setGstFileName(file.getOriginalFilename());
            merchant.setGstData(file.getBytes());
        }

        merchantRepository.save(merchant);
    }
}