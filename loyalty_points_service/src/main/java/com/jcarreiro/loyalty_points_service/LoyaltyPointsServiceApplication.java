package com.jcarreiro.loyalty_points_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class LoyaltyPointsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LoyaltyPointsServiceApplication.class, args);
  }
}
