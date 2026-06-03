package com.jcarreiro.loyalty_points_service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoyaltyPointsService {
    private final PurchaseRepository purchaseRepository;
    private final RewardRepository rewardRepository;
    private final PointsLotRepository pointsLotRepository;
    private final PointsTransactionRepository pointsTransactionRepository;
    private final LoyaltyTierRepository loyaltyTierRepository;
    private final Duration pointsValidDuration = Duration.ofDays(365);
    private final Duration trailingSpendDuration = Duration.ofDays(365);

    public LoyaltyPointsService(PurchaseRepository purchaseRepository, RewardRepository rewardRepository,
            PointsLotRepository pointsLotRepository, PointsTransactionRepository pointsTransactionRepository,
            LoyaltyTierRepository loyaltyTierRepository) {
        this.purchaseRepository = purchaseRepository;
        this.rewardRepository = rewardRepository;
        this.pointsLotRepository = pointsLotRepository;
        this.pointsTransactionRepository = pointsTransactionRepository;
        this.loyaltyTierRepository = loyaltyTierRepository;
    }

    /// Retrieves the current points balance for a customer's account.
    /// 
    /// @param accountId the ID of the customer's account
    /// @return the current points balance for the customer's account
    public int getPointsBalanceForAccount(String accountId) {
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
        final var expiryTime = Instant.now();
        final var balance = pointsLotRepository.getPointsBalanceForAccount(accountId, expiryTime);
        if (balance == null) {
            // There are no transactions for this account.
            return 0;
        }
        return balance;
    }

    /// Gets the loyalty tier for the customer.
    /// 
    /// The loyalty tier is based on the value of the customer's total spend over the
    /// past 12 months.
    String getLoyaltyTierForAccount(String accountId) {
        final var cutoff = Instant.now().minus(trailingSpendDuration);
        final var tier = loyaltyTierRepository.findTierForAccountSince(accountId, cutoff);
        return tier.map(LoyaltyTier::getTierName).orElse("");
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
        int pointsEarned = purchase.getDollarAmount().intValue();

        // Create a new lot for the new points.
        final var expiryTime = purchase.getTimestamp().plus(pointsValidDuration);
        PointsLot pl = new PointsLot(
                null,
                accountId,
                pointsEarned,
                expiryTime);
        pointsLotRepository.save(pl);

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
        final var currentBalance = getPointsBalanceForAccount(accountId);
        final var pointCost = reward.getPointCost();
        if (currentBalance < pointCost) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Customer does not have enough points to redeem reward: %s", rewardId));
        }

        // Spend points from lots, in FIFO order.
        int pointsNeeded = pointCost;
        var lots = pointsLotRepository.findSpendableLotsForAccount(accountId, Instant.now());
        var iterator = lots.iterator();
        while (iterator.hasNext() && pointsNeeded > 0) {
            // Spend either all the points left in the next lot, or the amount
            // remaining from the reward cost, whichever is least.
            var pl = iterator.next();
            int pointsInLot = pl.getPointsRemaining();
            int pointsSpent = Math.min(pointsInLot, pointsNeeded);
            pl.setPointsRemaining(pointsInLot - pointsSpent);
            pointsLotRepository.save(pl);
            pointsNeeded -= pointsSpent;
        }

        // We should have had enough lots to pay the reward cost, since we
        // check the balance up front.
        assert pointsNeeded == 0 : "Not enough points for reward!";

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