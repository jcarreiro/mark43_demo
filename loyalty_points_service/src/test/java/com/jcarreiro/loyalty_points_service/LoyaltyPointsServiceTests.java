package com.jcarreiro.loyalty_points_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// TODO(jcarreiro): these tests use the real clock right now; we should switch
// to using a mock clock so that the results of the expiry time tests are 
// deterministic.
class LoyaltyPointsServiceTests {
    @Test
    void getPointsBalance_returnsZeroWhenNoTransactions() {
        LoyaltyPointsService service = new LoyaltyPointsService(Map.of(), List.of());
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
        LoyaltyPointsService service = new LoyaltyPointsService(Map.of(), transactionLog);
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
                transactionLog);
        service.earnPoints("account-1", "purchase-1");
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.earnPoints("account-1", "purchase-1"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Points have already been earned from this purchase", exception.getReason());
    }
}
