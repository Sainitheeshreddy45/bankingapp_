package com.bankapp.portal.repository;
import com.bankapp.portal.model.MerchantImpersonation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ImpersonationRepository extends JpaRepository<MerchantImpersonation, Long> {
    Optional<MerchantImpersonation> findByAdminUsernameAndActiveTrue(String adminUsername);
}