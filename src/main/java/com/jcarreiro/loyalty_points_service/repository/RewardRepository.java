package com.jcarreiro.loyalty_points_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jcarreiro.loyalty_points_service.entity.Reward;

public interface RewardRepository extends JpaRepository<Reward, String> {
}