package com.jcarreiro.loyalty_points_service;

public record RewardMetadata(
    // The reward ID, e.g. 'free-coffee'.
    String rewardId,

    // The description of the reward, e.g. 'Get a free coffee after earning 
    // 100 points'.
    String description,

    // The point cost of the reward, e.g. 100.
    int pointCost
) {
}