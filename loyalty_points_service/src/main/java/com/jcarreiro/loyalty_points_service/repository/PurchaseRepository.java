package com.jcarreiro.loyalty_points_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jcarreiro.loyalty_points_service.entity.Purchase;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {
    List<Purchase> findByAccountId(String accountId);
}