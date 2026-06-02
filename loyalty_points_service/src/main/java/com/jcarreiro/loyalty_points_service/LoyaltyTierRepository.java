package com.jcarreiro.loyalty_points_service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

public interface LoyaltyTierRepository extends JpaRepository<LoyaltyTier, Long> {
    @NativeQuery(value = "SELECT lt.* FROM loyalty_tiers lt " +
            "WHERE lt.threshold_dollars <= (" +
            "  SELECT SUM(p.dollar_amount) " +
            "  FROM purchases p " +
            "  WHERE p.account_id = :accountId AND p.timestamp >= :cutoff " +
            ") " +
            "ORDER BY lt.threshold_dollars DESC LIMIT 1")
    Optional<LoyaltyTier> findTierForAccountSince(@Param("accountId") String accountId,
            @Param("cutoff") Instant cutoff);
}