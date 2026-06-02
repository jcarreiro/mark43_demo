package com.jcarreiro.loyalty_points_service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loyalty_tiers")
public class LoyaltyTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tierName;

    @Column(nullable = false)
    private Float thresholdDollars;

    public LoyaltyTier() {
        // Default constructor is required by JPA.
    }

    public LoyaltyTier(Long id, String tierName, Float thresholdDollars) {
        this.id = id;
        this.tierName = tierName;
        this.thresholdDollars = thresholdDollars;
    }

    public Long getId() {
        return id;
    }

    public String getTierName() {
        return tierName;
    }

    public Float getThresholdDollars() {
        return thresholdDollars;
    }
}
