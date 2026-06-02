package com.jcarreiro.loyalty_points_service;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    private String purchaseId;
    private String accountId;
    private Float dollarAmount;
    private Instant timestamp;

    protected Purchase() {
        // Default constructor required by JPA.
    }

    public Purchase(String purchaseId, String accountId, float dollarAmount, Instant timestamp) {
        this.purchaseId = purchaseId;
        this.accountId = accountId;
        this.dollarAmount = dollarAmount;
        this.timestamp = timestamp;
    }

    public String getAccountId() {
        return accountId;
    }

    public Float getDollarAmount() {
        return dollarAmount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
