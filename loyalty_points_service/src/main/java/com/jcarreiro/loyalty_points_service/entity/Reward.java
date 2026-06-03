package com.jcarreiro.loyalty_points_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rewards")
public class Reward {
    @Id
    String rewardId;
    String description;
    int pointCost;

    public Reward() {
        // Default constructor required by JPA.
    }

    public Reward(String rewardId, String description, int pointCost) {
        this.rewardId = rewardId;
        this.description = description;
        this.pointCost = pointCost;
    }

    public String getRewardId() {
        return rewardId;
    }

    public String getDescription() {
        return description;
    }

    public int getPointCost() {
        return pointCost;
    }
}
