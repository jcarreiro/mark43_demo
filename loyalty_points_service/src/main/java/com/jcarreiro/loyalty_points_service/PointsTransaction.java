package com.jcarreiro.loyalty_points_service;

import java.time.Instant;

public record PointsTransaction(
    // The transaction ID.
    String transactionId,

    // The account ID of the customer who made the transaction.
    String accountId,

    // The type of the transaction.
    TransactionType transactionType,

    // If the transaction is an EARN transaction, this field contains the 
    // purchaseId of the purchase from which the points were earned. This is
    // used to prevent users from earning points for the same purchase multiple
    // times. If the transaction is a REDEEM transaction, this field is null.
    String purchaseId,

    // The number of points involved in the transaction.
    int points,

    // The timestamp of the transaction. If this is an EARN transaction, this is
    // the timestamp of the original purchase. If this is a REDEEM transaction, 
    // this is the timestamp of the redemption.
    Instant timestamp
) {    
    public enum TransactionType {
        EARN,
        REDEEM
    }
}
