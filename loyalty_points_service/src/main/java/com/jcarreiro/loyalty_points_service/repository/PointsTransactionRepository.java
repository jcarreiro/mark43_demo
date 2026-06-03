package com.jcarreiro.loyalty_points_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jcarreiro.loyalty_points_service.entity.PointsTransaction;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    List<PointsTransaction> findByPurchaseId(String purchaseId);
}
