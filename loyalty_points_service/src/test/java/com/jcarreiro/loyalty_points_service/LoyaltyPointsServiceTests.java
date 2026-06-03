package com.jcarreiro.loyalty_points_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
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
        private PointsLotRepository pointsLotRepository;

        @Mock
        private PointsTransactionRepository pointsTransactionRepository;

        @Mock
        private LoyaltyTierRepository loyaltyTierRepository;

        @Test
        void getPointsBalance_returnsZeroWhenNoTransactions() {
                final var accountId = "account-1";
                when(pointsLotRepository.getPointsBalanceForAccount(eq(accountId), any(Instant.class)))
                                .thenReturn(null);
                LoyaltyPointsService service = new LoyaltyPointsService(purchaseRepository,
                                rewardRepository, pointsLotRepository, pointsTransactionRepository,
                                loyaltyTierRepository);
                int balance = service.getPointsBalanceForAccount(accountId);
                assertEquals(0, balance);
        }

        @Test
        void getPointsBalance_returnsCorrectBalance() {
                final String accountId = "account-1";
                // TODO(jcarreiro): mock the clock so we can verify the correct
                // expiry time is passed in to the mock
                final Integer expectedBalance = 70;
                when(pointsLotRepository.getPointsBalanceForAccount(eq(accountId), any(Instant.class)))
                                .thenReturn(expectedBalance);
                LoyaltyPointsService service = new LoyaltyPointsService(purchaseRepository,
                                rewardRepository, pointsLotRepository, pointsTransactionRepository,
                                loyaltyTierRepository);
                int balance = service.getPointsBalanceForAccount("account-1");
                assertEquals(70, balance);
        }

        @Test
        void getLoyaltyTier_returnsEmptyStringIfNoTierMatches() {
                final var accountId = "account-1";
                LoyaltyPointsService service = new LoyaltyPointsService(purchaseRepository,
                                rewardRepository, pointsLotRepository, pointsTransactionRepository,
                                loyaltyTierRepository);
                final var tier = service.getLoyaltyTierForAccount(accountId);
                assertEquals("", tier);
        }

        @Test
        void getLoyaltyTier_returnsCorrectTierName() {
                final var accountId = "account-1";
                final var expectedTierName = "Gold";
                final var tier = new LoyaltyTier(expectedTierName, 100.00f);
                when(loyaltyTierRepository.findTierForAccountSince(eq(accountId), any(Instant.class)))
                                .thenReturn(Optional.of(tier));
                LoyaltyPointsService service = new LoyaltyPointsService(purchaseRepository,
                                rewardRepository, pointsLotRepository, pointsTransactionRepository,
                                loyaltyTierRepository);
                final var actualTierName = service.getLoyaltyTierForAccount(accountId);
                assertEquals(expectedTierName, actualTierName);
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
                                pointsLotRepository,
                                pointsTransactionRepository,
                                loyaltyTierRepository);
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
                                pointsLotRepository,
                                pointsTransactionRepository,
                                loyaltyTierRepository);
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
                                pointsLotRepository,
                                pointsTransactionRepository,
                                loyaltyTierRepository);
                service.earnPoints(accountId, purchaseId);
                ArgumentCaptor<PointsLot> plCaptor = ArgumentCaptor.forClass(PointsLot.class);
                verify(pointsLotRepository).save(plCaptor.capture());
                PointsLot pl = plCaptor.getValue();
                // TODO(jcarreiro): mock the clock so we can assert on expiry time
                assertEquals(accountId, pl.getAccountId());
                assertEquals(50, pl.getPointsRemaining());

                ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
                verify(pointsTransactionRepository).save(txCaptor.capture());
                PointsTransaction tx = txCaptor.getValue();
                assertEquals(TransactionType.EARN, tx.getTransactionType());
                assertEquals(purchaseId, tx.getPurchaseId());
                assertEquals(accountId, tx.getAccountId());
                assertEquals(50, tx.getPoints());
                assertEquals(timestamp, tx.getTimestamp());
        }

        @Test
        void redeemPoints_throwsIfRewardIdIsInvalid() {
                final var accountId = "account-1";
                final var rewardId = "invalid-reward-id";
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsLotRepository,
                                pointsTransactionRepository,
                                loyaltyTierRepository);
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
                when(pointsLotRepository.getPointsBalanceForAccount(eq(accountId), any(Instant.class))).thenReturn(10);
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsLotRepository,
                                pointsTransactionRepository,
                                loyaltyTierRepository);
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
                when(pointsLotRepository.getPointsBalanceForAccount(eq(accountId), any(Instant.class)))
                                .thenReturn(pointCost);
                final var expiryTime = Instant.now().plus(Duration.ofDays(1));
                var pl = new PointsLot(1L, accountId, 200, expiryTime);
                when(pointsLotRepository.findSpendableLotsForAccount(eq(accountId), any(Instant.class)))
                                .thenReturn(List.of(pl));
                LoyaltyPointsService service = new LoyaltyPointsService(
                                purchaseRepository,
                                rewardRepository,
                                pointsLotRepository,
                                pointsTransactionRepository,
                                loyaltyTierRepository);
                service.redeemPoints("account-1", rewardId);
                assertEquals(100, pl.getPointsRemaining());
                ArgumentCaptor<PointsTransaction> captor = ArgumentCaptor.forClass(PointsTransaction.class);
                verify(pointsTransactionRepository).save(captor.capture());
                // TODO(jcarreiro): mock the clock so we can check the timestamp
                PointsTransaction captured = captor.getValue();
                assertEquals(TransactionType.REDEEM, captured.getTransactionType());
                assertEquals(accountId, captured.getAccountId());
                assertEquals(pointCost, -captured.getPoints());
        }
}
