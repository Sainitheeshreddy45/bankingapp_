package com.bankapp.portal.repository;

import com.bankapp.portal.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query(value = "SELECT * FROM transactions WHERE merchant_id = :merchantId " +
            "AND (:status IS NULL OR status = :status) " +
            "AND (:paymentMode IS NULL OR payment_mode = :paymentMode) " +
            "AND created_at BETWEEN :startDate AND :endDate",
            countQuery = "SELECT count(*) FROM transactions WHERE merchant_id = :merchantId " +
                    "AND (:status IS NULL OR status = :status) " +
                    "AND (:paymentMode IS NULL OR payment_mode = :paymentMode) " +
                    "AND created_at BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    Page<Transaction> findPaginatedTransactions(
            @Param("merchantId") String merchantId,
            @Param("status") String status,
            @Param("paymentMode") String paymentMode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.merchantId = :merchantId")
    Stream<Transaction> streamAllByMerchantId(@Param("merchantId") String merchantId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.merchantId = :merchantId AND t.createdAt >= :start")
    BigDecimal sumVolumeSince(@Param("merchantId") String merchantId, @Param("start") LocalDateTime start);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.merchantId = :merchantId AND t.createdAt >= :start")
    long countTotalSince(@Param("merchantId") String merchantId, @Param("start") LocalDateTime start);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.merchantId = :merchantId AND t.createdAt >= :start AND t.status = 'SUCCESS'")
    long countSuccessSince(@Param("merchantId") String merchantId, @Param("start") LocalDateTime start);
}