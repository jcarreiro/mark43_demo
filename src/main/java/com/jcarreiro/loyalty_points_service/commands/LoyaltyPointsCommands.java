package com.jcarreiro.loyalty_points_service.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.jcarreiro.loyalty_points_service.service.LoyaltyPointsService;

@ShellComponent
public class LoyaltyPointsCommands {

    @Autowired
    private LoyaltyPointsService loyaltyPointsService;

    @ShellMethod("Get points balance for a customer")
    public String balance(@ShellOption String accountId) {
        final var balance = loyaltyPointsService.getPointsBalanceForAccount(accountId);
        final var tier = loyaltyPointsService.getLoyaltyTierForAccount(accountId);
        return String.format("Account %s balance is %d points, tier %s.", accountId, balance, tier);
    }

    @ShellMethod("Add points to a customer's account for a purchase")
    public String earn(@ShellOption String accountId, @ShellOption String purchaseId) {
        loyaltyPointsService.earnPoints(accountId, purchaseId);
        final var balance = loyaltyPointsService.getPointsBalanceForAccount(accountId);
        return String.format("Points for purchase %s added to account %s. New account balance is %d points.",
                purchaseId, accountId, balance);
    }

    @ShellMethod("Redeem a reward using points")
    public String redeem(@ShellOption String accountId, @ShellOption String rewardId) {
        loyaltyPointsService.redeemPoints(accountId, rewardId);
        final var balance = loyaltyPointsService.getPointsBalanceForAccount(accountId);
        return String.format("Reward %s redeemed for account %s. New account balance is %d points.", rewardId,
                accountId, balance);
    }

    @ShellMethod("Clawback points for a refunded purchase from a customer account")
    public String clawback(@ShellOption String accountId, @ShellOption String purchaseId) {
        loyaltyPointsService.clawbackPoints(accountId, purchaseId);
        final var balance = loyaltyPointsService.getPointsBalanceForAccount(accountId);
        return String.format(
                "Points for purchase %s have been removed (clawed back) from account %s. New account balance is %d points.",
                purchaseId, accountId, balance);
    }
}
