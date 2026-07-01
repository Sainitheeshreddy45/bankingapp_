package com.bankapp.portal.repository;

import com.bankapp.portal.model.IdempotentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotentRequest, String> {
}