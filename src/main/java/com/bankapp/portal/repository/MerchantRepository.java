package com.bankapp.portal.repository;
import com.bankapp.portal.model.Merchant;
import com.bankapp.portal.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    /**
     * 🔍 Core Lifecycle Lookup Point:
     * Locates a business entity profile using the authenticated user's
     * email address extracted out of the security token context.
     */
    Optional<Merchant> findByOwnerEmail(String ownerEmail);
}