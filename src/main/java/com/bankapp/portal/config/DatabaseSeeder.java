package com.bankapp.portal.config;

import com.bankapp.portal.model.*;
import com.bankapp.portal.repository.MerchantRepository;
import com.bankapp.portal.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final MerchantRepository merchantRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    @Override
    public void run(String... args) throws Exception {
        if (merchantRepository.count() == 0) {
            Merchant acme = new Merchant();
            acme.setBusinessName("Acme Payments Ltd");
            acme.setOwnerEmail("acme@test.com");
            acme.setCompanyType("PRIVATE_LIMITED");
            acme.setKycStatus(KycStatus.SUBMITTED);
            merchantRepository.save(acme);
        }

        if (adminAuditLogRepository.count() == 0) {
            // Using your clear matching tracking structures
            AdminAuditLog schemaSeedEntry = new AdminAuditLog(
                    "system_seeder@bank.com",
                    "SYSTEM_INITIALIZATION",
                    "127.0.0.1",
                    "{}",
                    "{\"status\":\"SEED_COMPLETE\"}"
            );
            adminAuditLogRepository.save(schemaSeedEntry);
        }
    }
}
