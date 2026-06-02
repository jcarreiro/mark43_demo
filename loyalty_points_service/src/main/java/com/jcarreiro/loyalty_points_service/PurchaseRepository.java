package com.jcarreiro.loyalty_points_service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {
    List<Purchase> findByAccountId(String accountId);
}