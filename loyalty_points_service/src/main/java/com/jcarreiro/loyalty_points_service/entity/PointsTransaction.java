package com.jcarreiro.loyalty_points_service.entity;

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
        REDEEM,
        CLAWBACK,
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
    // - If this is a REDEEM or CLAWBACK transaction, the timestamp of the
    // redemption or clawback.
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

    public Long getTransactionId() {
        return transactionId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public Integer getPoints() {
        return points;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
