package com.jcarreiro.loyalty_points_service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loyalty_tiers")
public class LoyaltyTier {
    @Id
    private String tierName;

    @Column(nullable = false)
    private Float thresholdDollars;

    public LoyaltyTier() {
        // Default constructor is required by JPA.
    }

    public LoyaltyTier(String tierName, Float thresholdDollars) {
        this.tierName = tierName;
        this.thresholdDollars = thresholdDollars;
    }

    public String getTierName() {
        return tierName;
    }

    public Float getThresholdDollars() {
        return thresholdDollars;
    }
}
