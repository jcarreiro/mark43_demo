package com.jcarreiro.loyalty_points_service;

public record PointsBalanceResponse(String accountId, int points, String loyaltyTierName) {
}
