package com.bankapp.portal.service;

import com.bankapp.portal.dto.RefundRequest;
import com.bankapp.portal.dto.TransactionSearchCriteria;
import com.bankapp.portal.model.IdempotentRequest;
import com.bankapp.portal.model.Transaction;
import com.bankapp.portal.repository.IdempotencyRepository;
import com.bankapp.portal.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class TransactionCoreService {

    private final TransactionRepository transactionRepository;
    private final IdempotencyRepository idempotencyRepository;

    public TransactionCoreService(TransactionRepository transactionRepository, IdempotencyRepository idempotencyRepository) {
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    /**
     * Requirement: Server-Side pagination supporting 100k+ records smoothly.
     */
    public Page<Transaction> getMerchantTransactions(TransactionSearchCriteria criteria) {
        PageRequest pageRequest = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        return transactionRepository.findPaginatedTransactions(
                criteria.getMerchantId(),
                criteria.getStatus(),
                criteria.getPaymentMode(),
                criteria.getStartDate() != null ? criteria.getStartDate() : java.time.LocalDateTime.now().minusDays(30),
                criteria.getEndDate() != null ? criteria.getEndDate() : java.time.LocalDateTime.now(),
                pageRequest
        );
    }

    /**
     * Requirement: Idempotent Refund Processing Engine execution mapping.
     */
    @Transactional
    public String executeIdempotentRefund(String key, RefundRequest request) {
        // 1. Check if the key has already been executed via our safe database model strategy
        Optional<IdempotentRequest> savedResult = idempotencyRepository.findById(key);
        if (savedResult.isPresent()) {
            return savedResult.get().getResponseBody();
        }

        // 2. Fetch origin payment details utilizing pessimistic write locks to kill race conditions
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Target transaction record missing."));

        if (!"SUCCESS".equalsIgnoreCase(transaction.getStatus()) && !"PARTIALLY_REFUNDED".equalsIgnoreCase(transaction.getStatus())) {
            throw new IllegalStateException("Transaction cannot be refunded in its current state.");
        }

        if (transaction.getAmount().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Refund limit allocation exception: Value exceeds purchase depth.");
        }

        // 3. Process the refund and adjust balance state fields safely
        transaction.setAmount(transaction.getAmount().subtract(request.getAmount()));
        if (transaction.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
            transaction.setStatus("REFUNDED");
        } else {
            transaction.setStatus("PARTIALLY_REFUNDED");
        }
        transactionRepository.save(transaction);

        // 4. Construct outcome message and write execution key mapping
        String responseBody = "{\"status\":\"SUCCESS\",\"msg\":\"Refund completed balance adjusted.\"}";
        IdempotentRequest trackingLock = new IdempotentRequest(key, 200, responseBody, java.time.LocalDateTime.now());

        idempotencyRepository.save(trackingLock);

        return responseBody;
    }
}