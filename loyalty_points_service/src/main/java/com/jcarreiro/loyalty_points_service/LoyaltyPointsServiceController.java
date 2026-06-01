package com.jcarreiro.loyalty_points_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
public class LoyaltyPointsServiceController {
    @GetMapping("/{accountId}/balance")
    public PointsBalanceResponse getPointsBalance(@PathVariable String accountId) {
        // TODO(jcarreiro): Implement actual logic to retrieve points balance for the given accountId
        // TODO(jcarreiro): validate accountId in service layer and return appropriate error response if accountId is invalid
        return new PointsBalanceResponse(accountId, 100);
    }

    @PostMapping("/{accountId}/earn")
    public void earnPoints(@PathVariable String accountId) {
        // TODO(jcarreiro): Implement actual logic to add points to the given accountId
    }

    @PostMapping("/{accountId}/redeem")
    public void redeemPoints(@PathVariable String accountId) {
        // TODO(jcarreiro): Implement actual logic to redeem points from the given accountId
    }
}
