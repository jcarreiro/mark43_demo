package com.jcarreiro.loyalty_points_service;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    List<PointsTransaction> findByPurchaseId(String purchaseId);
}
