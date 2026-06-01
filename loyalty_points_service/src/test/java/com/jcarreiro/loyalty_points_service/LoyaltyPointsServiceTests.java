package com.jcarreiro.loyalty_points_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

// TODO(jcarreiro): these tests use the real clock right now; we should switch
// to using a mock clock so that the results of the expiry time tests are 
// deterministic.
class LoyaltyPointsServiceTests {
    @Test
    void getPointsBalance_returnsZeroWhenNoTransactions() {
        LoyaltyPointsService service = new LoyaltyPointsService(Map.of(), Map.of(), List.of());
        int balance = service.getPointsBalance("account-1");
        assertEquals(0, balance);
    }

    @Test
    void getPointsBalance_returnsCorrectBalance() {
        List<PointsTransaction> transactionLog = List.of(
                // This transaction is expired, so it should not contribute to 
                // the balance.
                new PointsTransaction(
                        "txn1",
                        "account-1",
                        PointsTransaction.TransactionType.EARN,
                        "purchase-1",
                        50,
                        Instant.parse("2025-05-31T00:00:00Z")),
                new PointsTransaction(
                        "txn2",
                        "account-1",
                        PointsTransaction.TransactionType.EARN,
                        "purchase-1",
                        100,
                        Instant.parse("2026-06-01T00:00:00Z")),
                new PointsTransaction(
                        "txn3",
                        "account-1",
                        PointsTransaction.TransactionType.REDEEM,
                        null,
                        -30,
                        Instant.parse("2026-06-01T00:00:00Z")));
        LoyaltyPointsService service = new LoyaltyPointsService(Map.of(), Map.of(), transactionLog);
        int balance = service.getPointsBalance("account-1");
        assertEquals(70, balance);
    }

    @Test
    void earnPoints_throwsWhenSamePurchaseIdIsUsedTwice() {
        PurchaseMetadata purchase = new PurchaseMetadata(
                "purchase-1",
                "account-1",
                50.0f,
                Instant.parse("2026-06-01T00:00:00Z"));
        List<PointsTransaction> transactionLog = new ArrayList<>();
        LoyaltyPointsService service = new LoyaltyPointsService(
                Map.of("purchase-1", purchase),
                Map.of(),
                transactionLog);
        service.earnPoints("account-1", "purchase-1");
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.earnPoints("account-1", "purchase-1"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Points have already been earned from this purchase", exception.getReason());
    }

    @Test
    void redeemPoints_throwsIfRewardIdIsInvalid() {
        LoyaltyPointsService service = new LoyaltyPointsService(Map.of(), 
        Map.of(
                "valid-reward-id", new RewardMetadata(
                        "valid-reward-id",
                        "Valid Reward",
                        100)
        ), List.of());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.redeemPoints("account-1", "invalid-reward-id"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid reward ID: invalid-reward-id", exception.getReason());
    }

    @Test
    void redeemPoints_throwsIfCustomerDoesNotHaveEnoughPoints() {
        LoyaltyPointsService service = new LoyaltyPointsService(Map.of(), 
        Map.of(
                "expensive-reward-id", new RewardMetadata(
                        "expensive-reward-id",
                        "Expensive Reward",
                        1000)
        ), List.of());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.redeemPoints("account-1", "expensive-reward-id"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Customer does not have enough points to redeem reward: expensive-reward-id", exception.getReason());
    }

    @Test
    void redeemPoints_redeemsPointsSuccessfully() {
        List<PointsTransaction> transactionLog = new ArrayList<>();
        transactionLog.add(new PointsTransaction(
                "txn1",
                "account-1",
                PointsTransaction.TransactionType.EARN,
                "purchase-1",
                500,
                Instant.parse("2026-06-01T00:00:00Z")));
        LoyaltyPointsService service = new LoyaltyPointsService(Map.of(), 
        Map.of(
                "reward-id", new RewardMetadata(
                        "reward-id",
                        "Reward",
                        100)
        ), transactionLog);
        service.redeemPoints("account-1", "reward-id");
        int balance = service.getPointsBalance("account-1");
        assertEquals(400, balance);
    }
}
