package com.jcarreiro.loyalty_points_service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointsLotRepository extends JpaRepository<PointsLot, Long> {
        @NativeQuery(value = "SELECT SUM(points_remaining) FROM points_lots " +
                        "WHERE account_id = :accountId AND expiry_time > :expiryTime")
        public Integer getPointsBalanceForAccount(@Param("accountId") String accountId,
                        @Param("expiryTime") Instant expiryTime);

        public List<PointsLot> findByAccountId(String accountId);

        @Query("select pl from PointsLot pl where pl.accountId = :accountId and pl.expiryTime > :expiryTime and pl.pointsRemaining > 0")
        public List<PointsLot> findSpendableLotsForAccount(@Param("accountId") String accountId,
                        @Param("expiryTime") Instant expiryTime);

        // TODO(jcarreiro): I'm not sure why we need to have the aliases in this
        // query, when we don't need them in the LoyaltyTierRepository?
        // @NativeQuery(value = "SELECT " +
        // " pl.id, " +
        // " pl.account_id as accountId, " +
        // " pl.points_remaining as pointsRemaining, " +
        // " pl.expiry_time as expiryTime " +
        // "FROM points_lots pl " +
        // "WHERE " +
        // " pl.account_id = :accountId AND " +
        // " pl.expiry_time > :expiryTime AND " +
        // " pl.points_remaining > 0 " +
        // "ORDER BY expiry_time ASC")
        // @Query("select pl from PointsLot pl where pl.accountId = :accountId")
        // public List<PointsLot> getSpendableLots(@Param("accountId") String accountId,
        // @Param("expiryTime") Instant expiryTime);
}
