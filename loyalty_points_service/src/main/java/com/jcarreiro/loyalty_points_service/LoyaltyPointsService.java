package com.jcarreiro.loyalty_points_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;

@Service
public class LoyaltyPointsService {
    private final PurchaseRepository purchaseRepository;
    private final RewardRepository rewardRepository;
    private final PointsTransactionRepository pointsTransactionRepository;
    private final Duration pointsValidDuration = Duration.ofDays(365);

    public LoyaltyPointsService(PurchaseRepository purchaseRepository, RewardRepository rewardRepository,
            PointsTransactionRepository pointsTransactionRepository) {
        this.purchaseRepository = purchaseRepository;
        this.rewardRepository = rewardRepository;
        this.pointsTransactionRepository = pointsTransactionRepository;
    }

    /// Retrieves the current points balance for a customer's account.
    /// 
    /// @param accountId the ID of the customer's account
    /// @return the current points balance for the customer's account
    public int getPointsBalance(String accountId) {
        // TODO(jcarreiro): check accountId is valid

        // Get the current points balance for the given accountId by iterating
        // through the transaction log. We add points for any EARN transactions
        // that have not yet expired, and subtract points for any REDEEM
        // transactions. For this demo, we assume that points expire 1 year
        // after they were earned, but for a real application, the expiry logic
        // may be more complex and could involve looking up the expiry policy
        // from a database. Also, in a real application, you would likely want
        // to optimize this by maintaining a separate data structure that tracks
        // the current points balance for each accountId, rather than iterating
        // through the entire transaction log each time.
        Instant expiryTime = Instant.now().minus(pointsValidDuration);
        var balance = pointsTransactionRepository.getPointBalance(accountId, expiryTime);
        if (balance == null) {
            // There are no transactions for this account.
            return 0;
        }
        return balance;
    }

    /// Add points to a customer's account.
    /// 
    /// Adds points earned by the given purchaseId to the customer's account.
    /// 
    /// @param accountId  the ID of the customer's account
    /// @param purchaseId the ID of the purchase that earned the points
    public void earnPoints(String accountId, String purchaseId) {
        // TODO(jcarreiro): check accountId is valid

        // Make sure the purchaseId is valid and corresponds to a purchase made
        // by the given accountId.
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid purchase ID"));
        if (!purchase.getAccountId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid purchase ID");
        }

        // If the purchase is too old, then the user can no longer earn the
        // points from it. Note that we also filter expired points when getting
        // the balance, so this is really just an optimization.
        final var expiryTime = Instant.now().minus(pointsValidDuration);
        if (purchase.getTimestamp().isBefore(expiryTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase is too old, points can no longer be earned");
        }

        // Make sure the user has not already earned points from this purchaseId.
        //
        // TODO(jcarreiro): JPA doesn't know purchaseId is a FK so we get a list
        // here.
        var pointsTransaction = pointsTransactionRepository.findByPurchaseId(purchaseId);
        if (!pointsTransaction.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Points have already been earned from this purchase");
        }

        // Calculate the number of points to earn from the purchase. For this
        // demo, we just use a hard-coded conversion rate of 1 point per dollar
        // spent, but for a real application, this may be more complex and could
        // involve looking up the conversion rate from a database or another
        // service.
        int pointsEarned = (int) purchase.getDollarAmount();

        // Add a new transaction to the transaction log to record earning the
        // points.
        PointsTransaction tx = new PointsTransaction(
                null,
                PointsTransaction.TransactionType.EARN,
                accountId,
                purchaseId,
                pointsEarned,
                purchase.getTimestamp());
        pointsTransactionRepository.save(tx);
    }

    /// Redeem points from a customer's account.
    /// 
    /// Redeems points from the customer's account for a reward.
    /// 
    /// @param accountId the ID of the customer's account
    /// @param rewardId  the ID of the reward for which the customer wants to
    ///                  redeem points}
    public void redeemPoints(String accountId, String rewardId) {
        // TODO(jcarreiro): check accountId is valid

        // Look up the reward metadata for the given rewardId.
        Reward reward = rewardRepository.findById(rewardId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Invalid reward ID: %s", rewardId)));

        // Check if the customer has enough points to redeem the reward.
        int currentBalance = getPointsBalance(accountId);
        int pointCost = reward.getPointCost();
        if (currentBalance < pointCost) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Customer does not have enough points to redeem reward: %s", rewardId));
        }

        // Add new transaction to the transaction log to record the redemption.
        PointsTransaction tx = new PointsTransaction(
                null,
                PointsTransaction.TransactionType.REDEEM,
                accountId,
                null,
                -pointCost,
                Instant.now());
        pointsTransactionRepository.save(tx);
    }
}