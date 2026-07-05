package com.pluresideas.fidelitytradingtracker.model;

import java.math.BigDecimal;

/**
 * Represents a single trading transaction parsed from the Fidelity history CSV.
 */
public record Transaction(
        String date,
        String account,
        String accountNum,
        Action action,
        String symbol,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal amount
) {}
