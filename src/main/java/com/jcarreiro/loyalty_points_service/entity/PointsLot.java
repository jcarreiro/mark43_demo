package com.jcarreiro.loyalty_points_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "points_lots")
public class PointsLot {
    // The lot ID.
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long lotId;

    // The account ID that owns this lot.
    String accountId;

    // The count of points remaining in this lot.
    Integer pointsRemaining;

    // The expiry time for this lot.
    Instant expiryTime;

    public PointsLot() {
        // Default constructor required by JPA.
    }

    public PointsLot(Long lotId, String accountId, Integer pointsRemaining, Instant expiryTime) {
        this.lotId = lotId;
        this.accountId = accountId;
        this.pointsRemaining = pointsRemaining;
        this.expiryTime = expiryTime;
    }

    public Long getLotId() {
        return lotId;
    }

    public String getAccountId() {
        return accountId;
    }

    public Integer getPointsRemaining() {
        return pointsRemaining;
    }

    public void setPointsRemaining(Integer pointsRemaining) {
        this.pointsRemaining = pointsRemaining;
    }

    public Instant getExpiryTime() {
        return expiryTime;
    }
}
