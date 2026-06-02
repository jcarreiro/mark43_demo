package com.jcarreiro.loyalty_points_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.jcarreiro.loyalty_points_service.PointsTransaction.TransactionType;

// TODO(jcarreiro): these tests use the real clock right now; we should switch
// to using a mock clock so that the results of the expiry time tests are 
// deterministic.
@ExtendWith(MockitoExtension.class)
class LoyaltyPointsServiceTests {
        @Mock
        private PurchaseRepository purchaseRepository;

        @Mock
        private RewardRepository rewardRepository;

        @Mock
        private PointsTransactionRepository pointsTransactionRepository;

        @Test
        void getPointsBalance_returnsZeroWhenNoTransactions() {
                final var accountId = "account-1";
                when(pointsTransactionRepository.getPointBalance(eq(accountId), any(Instant.class))).thenReturn(null);
                LoyaltyPointsService service = new LoyaltyPointsService(purchaseRepository,
                                rewardRepository, pointsTransactionRepository);
                int balance = service.getPointsBalance(accountId);
                assertEquals(0, balance);
        }

        @Test
        void getPointsBalance_returnsCorrectBalance() {
                final String accountId = "account-1";
                // TODO(jcarreiro): mock the clock so we can verify the correct
                // expiry time is passed in to the mock
                final Integer expectedBalance = 70;
                when(pointsTransactionRepository.getPointBalance(eq(accountId), any(Instant.class)))
                                .thenReturn(expectedBalance);
                LoyaltyPointsService service = new LoyaltyPointsService(purchaseRepository,
                                rewardRepository, pointsTransactionRepository);
                int balance = service.getPointsBalance("account-1");
                assertEquals(70, balance);
        }

        @Test
        void earnPoints_throwsWhenSamePurchaseIdIsUsedTwice() {
                final var accountId = "account-1";
                final var purchaseId = "purchase-1";
                final var purchase = new Purchase(
                                purchaseId,
                                accountId,
                                50.0f,
                                Instant.parse("2026-06-01T00:00:00Z"));
                when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
                final var tx = new PointsTransaction(
                                123L,
                                TransactionType.EARN,
                                accountId,
                                purchaseId,
                                100,
                                Instant.now());
                when(pointsTransactionRepository.findByPurchaseId(purchaseId)).thenReturn(List.of(tx));
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsTransactionRepository);
                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.earnPoints(accountId, purchaseId));
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                assertEquals("Points have already been earned from this purchase", exception.getReason());
        }

        @Test
        void earnPoints_throwsWhenPurchaseWasForDifferentAccount() {
                final var accountId = "account-1";
                final var otherAccountId = "account-2";
                final var purchaseId = "purchase-1";
                final var purchase = new Purchase(
                                purchaseId,
                                otherAccountId,
                                50.0f,
                                Instant.parse("2026-06-01T00:00:00Z"));
                when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsTransactionRepository);
                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.earnPoints(accountId, purchaseId));
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                assertEquals("Invalid purchase ID", exception.getReason());
        }

        @Test
        void earnPoints_recordsTransactionForValidPurchase() {
                final var accountId = "account-1";
                final var purchaseId = "purchase-1";
                final var timestamp = Instant.parse("2026-06-01T00:00:00Z");
                final var purchase = new Purchase(
                                purchaseId,
                                accountId,
                                50.0f,
                                timestamp);
                when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchase));
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsTransactionRepository);
                service.earnPoints(accountId, purchaseId);
                ArgumentCaptor<PointsTransaction> captor = ArgumentCaptor.forClass(PointsTransaction.class);
                verify(pointsTransactionRepository).save(captor.capture());
                PointsTransaction captured = captor.getValue();
                assertEquals(TransactionType.EARN, captured.getTransactionType());
                assertEquals(purchaseId, captured.getPurchaseId());
                assertEquals(accountId, captured.getAccountId());
                assertEquals(50, captured.getPoints());
                assertEquals(timestamp, captured.getTimestamp());
        }

        @Test
        void redeemPoints_throwsIfRewardIdIsInvalid() {
                final var accountId = "account-1";
                final var rewardId = "invalid-reward-id";
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsTransactionRepository);
                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.redeemPoints(accountId, rewardId));
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                assertEquals(String.format("Invalid reward ID: %s", rewardId), exception.getReason());
        }

        @Test
        void redeemPoints_throwsIfCustomerDoesNotHaveEnoughPoints() {
                final var accountId = "account-1";
                final var rewardId = "expensive-reward-id";
                final var reward = new Reward(rewardId, "Expensive Reward", 1000);
                when(rewardRepository.findById(rewardId)).thenReturn(Optional.of(reward));
                when(pointsTransactionRepository.getPointBalance(eq(accountId), any(Instant.class))).thenReturn(10);
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsTransactionRepository);
                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> service.redeemPoints(accountId, rewardId));
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                assertEquals(String.format("Customer does not have enough points to redeem reward: %s", rewardId),
                                exception.getReason());
        }

        @Test
        void redeemPoints_recordsTransactionIfRewardRedeemedSuccessfully() {
                final var accountId = "account-1";
                final var rewardId = "reward-id";
                final var pointCost = 100;
                final var reward = new Reward(rewardId, "Reward", pointCost);
                when(rewardRepository.findById(rewardId)).thenReturn(Optional.of(reward));
                when(pointsTransactionRepository.getPointBalance(eq(accountId), any(Instant.class)))
                                .thenReturn(pointCost);
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsTransactionRepository);
                service.redeemPoints("account-1", rewardId);
                ArgumentCaptor<PointsTransaction> captor = ArgumentCaptor.forClass(PointsTransaction.class);
                verify(pointsTransactionRepository).save(captor.capture());
                // TODO(jcarreiro): mock the clock so we can check the timestamp
                // of the transaction here
                PointsTransaction captured = captor.getValue();
                assertEquals(TransactionType.REDEEM, captured.getTransactionType());
                assertEquals(accountId, captured.getAccountId());
                assertEquals(pointCost, -captured.getPoints());
        }
}
