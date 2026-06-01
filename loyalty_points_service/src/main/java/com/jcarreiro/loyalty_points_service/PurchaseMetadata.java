package com.jcarreiro.loyalty_points_service;

import java.time.Instant;

public record PurchaseMetadata(
    // The purchase ID.
    String purchaseId,

    // The account ID of the customer who made the purchase. This is used to
    // prevent users from earning points from purchases that they did not make.
    String accountId,

    // The dollar amount of the purchase. This is used to calculate how many 
    // points the user should earn from the purchase.
    float dollarAmount,

    // The timestamp of the purchase. This is used when handling expiry of
    // unused points.
    Instant purchaseTimestamp
) {
    
}
