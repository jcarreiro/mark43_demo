package com.jcarreiro.loyalty_points_service;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    @Query("""
            select sum(t.points) from PointsTransaction t
            where t.accountId = :accountId
            and (
                t.transactionType = com.jcarreiro.loyalty_points_service.PointsTransaction.TransactionType.REDEEM
                or (
                    t.transactionType = com.jcarreiro.loyalty_points_service.PointsTransaction.TransactionType.EARN
                    and t.timestamp >= :expiryTime
                )
            )
            """)
    Integer getPointBalance(
            @Param("accountId") String accountId,
            @Param("expiryTime") Instant expiryTime);

    List<PointsTransaction> findByPurchaseId(String purchaseId);
}
