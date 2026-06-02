package com.jcarreiro.loyalty_points_service;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRepository extends JpaRepository<Reward, String> {
}