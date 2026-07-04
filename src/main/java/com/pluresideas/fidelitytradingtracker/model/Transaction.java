package com.pluresideas.fidelitytradingtracker.model;

/**
 * Represents a single trading transaction parsed from the Fidelity history CSV.
 */
public record Transaction(
        String date,
        String account,
        String accountNum,
        Action action,
        String symbol,
        double price,
        double quantity,
        double amount
) {
}
