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
    // Map from purchaseId to purchase metadata. This is used to allow users
    // to earn points from past purchases, and to prevent users from earning
    // points multiple times from the same purchase. In a real application,
    // this could be stored in a database or retrieved from another service.
    private final Map<String, PurchaseMetadata> purchaseMap;

    // Log of all points transactions. In a real application, this would likely
    // be stored in a database.
    private final List<PointsTransaction> transactionLog;

    @Autowired
    public LoyaltyPointsService() {
        this(defaultPurchaseMap(), new ArrayList<>());
    }

    LoyaltyPointsService(Map<String, PurchaseMetadata> purchaseMap, List<PointsTransaction> transactionLog) {
        this.purchaseMap = purchaseMap;
        this.transactionLog = transactionLog;
    }

    private static Map<String, PurchaseMetadata> defaultPurchaseMap() {
        return Map.of(
                "purchase1", new PurchaseMetadata(
                        "purchase1",
                        "account1",
                        50.0f,
                        Instant.parse("2026-06-01T00:00:00Z")),
                "purchase2", new PurchaseMetadata(
                        "purchase2",
                        "account1",
                        25.0f,
                        Instant.parse("2025-05-31T00:00:00Z")),
                "purchase3", new PurchaseMetadata(
                        "purchase3",
                        "account2",
                        100.0f,
                        Instant.parse("2026-06-01T00:00:00Z")));
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
        int balance = 0;
        for (PointsTransaction transaction : transactionLog) {
            if (transaction.accountId().equals(accountId)) {
                if (transaction.transactionType() == PointsTransaction.TransactionType.EARN) {
                    // Check if the points from this transaction have expired.
                    // For this demo, we assume that points expire 1 year after
                    // they were earned.
                    Duration oneYear = Duration.ofDays(365);
                    Instant expiryTime = transaction.timestamp().plus(oneYear);
                    if (Instant.now().isAfter(expiryTime)) {
                        // Points have expired, so we skip this transaction.
                        continue;
                    }
                }
                balance += transaction.points();
            }
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
        PurchaseMetadata purchaseMetadata = purchaseMap.get(purchaseId);
        if (purchaseMetadata == null || !purchaseMetadata.accountId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid purchaseId or accountId");
        }

        // Make sure the user has not already earned points from this purchaseId.
        for (PointsTransaction transaction : transactionLog) {
            if (transaction.purchaseId() != null && transaction.purchaseId().equals(purchaseId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Points have already been earned from this purchase");
            }
        }

        // Calculate the number of points to earn from the purchase. For this
        // demo, we just use a hard-coded conversion rate of 1 point per dollar
        // spent, but for a real application, this may be more complex and could
        // involve looking up the conversion rate from a database or another
        // service.
        int pointsEarned = (int) purchaseMetadata.dollarAmount();

        // Add a new transaction to the transaction log.
        PointsTransaction transaction = new PointsTransaction(
                /* transactionId= */ String.format("txn%d", transactionLog.size() + 1),
                /* accountId= */ accountId,
                /* transactionType= */ PointsTransaction.TransactionType.EARN,
                /* purchaseId= */ purchaseId,
                /* points= */ pointsEarned,
                /* timestamp= */ purchaseMetadata.purchaseTimestamp());
        transactionLog.add(transaction);
    }

    /// Redeem points from a customer's account.
    /// 
    /// Redeems points from the customer's account for a reward.
    /// 
    /// @param accountId the ID of the customer's account
    /// @param rewardId  the ID of the reward for which the customer wants to
    ///                  redeem points}
    public void redeemPoints(String accountId, String rewardId) {
        // TODO(jcarreiro): add a reward catalog
        // TODO(jcarreiro): Implement actual logic to redeem points from the given
        // accountId
    }
}