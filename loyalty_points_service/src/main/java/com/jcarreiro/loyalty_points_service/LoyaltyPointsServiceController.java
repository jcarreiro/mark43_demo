package com.jcarreiro.loyalty_points_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
public class LoyaltyPointsServiceController {
    private final LoyaltyPointsService loyaltyPointsService;

    public LoyaltyPointsServiceController(LoyaltyPointsService loyaltyPointsService) {
        this.loyaltyPointsService = loyaltyPointsService;
    }

    @GetMapping("/{accountId}/balance")
    public PointsBalanceResponse getPointsBalance(@PathVariable String accountId) {
        int balance = loyaltyPointsService.getPointsBalance(accountId);
        return new PointsBalanceResponse(accountId, balance);
    }

    @PostMapping("/{accountId}/earn")
    public void earnPoints(@PathVariable String accountId, @RequestBody EarnPointsRequest request) {
        loyaltyPointsService.earnPoints(accountId, request.purchaseId());
    }

    @PostMapping("/{accountId}/redeem")
    public void redeemPoints(@PathVariable String accountId) {
        // TODO(jcarreiro): Implement actual logic to redeem points from the given
        // accountId
    }
}
