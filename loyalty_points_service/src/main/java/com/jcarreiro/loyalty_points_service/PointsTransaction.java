package com.jcarreiro.loyalty_points_service;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// TODO(jcarreiro): record the rewardId for REDEEM transactions
@Entity
@Table(name = "transactions")
public class PointsTransaction {
    public enum TransactionType {
        EARN,
        REDEEM
    }

    // The transaction ID.
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long transactionId;

    // The type of the transaction.
    @Enumerated(EnumType.STRING)
    TransactionType transactionType;

    // The account ID of the customer who made the transaction.
    String accountId;

    // If the transaction is an EARN transaction, this field contains the
    // purchaseId of the purchase from which the points were earned. This is
    // used to prevent users from earning points for the same purchase multiple
    // times. If the transaction is a REDEEM transaction, this field is null.
    String purchaseId;

    // The number of points involved in the transaction.
    Integer points;

    // The timestamp of the transaction:
    //
    // - If this is an EARN transaction, this is the timestamp of the original
    // purchase.
    //
    // - If this is a REDEEM transaction, the timestamp of the redemption.
    Instant timestamp;

    public PointsTransaction() {
        // Default constructor required by JPA.
    }

    public PointsTransaction(Long transactionId, TransactionType transactionType, String accountId, String purchaseId,
            int points, Instant timestamp) {
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.accountId = accountId;
        this.purchaseId = purchaseId;
        this.points = points;
        this.timestamp = timestamp;
    }

    Long getTransactionId() {
        return transactionId;
    }

    TransactionType getTransactionType() {
        return transactionType;
    }

    String getAccountId() {
        return accountId;
    }

    String getPurchaseId() {
        return purchaseId;
    }

    Integer getPoints() {
        return points;
    }

    Instant getTimestamp() {
        return timestamp;
    }
}
