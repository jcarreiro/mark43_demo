package com.jcarreiro.loyalty_points_service.dto;

public record PointsBalanceResponse(String accountId, int points, String loyaltyTierName) {
}
