package com.pluresideas.fidelitytradingtracker.model;

import java.math.BigDecimal;

public class Account {
    private final String name;
    private final String accountNumber;
    private int tradesCount;
    private BigDecimal buysValue = BigDecimal.ZERO;
    private BigDecimal sellsValue = BigDecimal.ZERO;

    public Account(String name, String accountNumber) {
        this.name = name;
        this.accountNumber = accountNumber;
    }

    public void addTrade(Action action, BigDecimal amount) {
        tradesCount++;
        if (action == Action.BUY) {
            buysValue = buysValue.add(amount);
        } else if (action == Action.SELL) {
            sellsValue = sellsValue.add(amount);
        }
    }

    public String getName() {
        return name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public int getTradesCount() {
        return tradesCount;
    }

    public BigDecimal getBuysValue() {
        return buysValue;
    }

    public BigDecimal getSellsValue() {
        return sellsValue;
    }

    public BigDecimal getNetCashFlow() {
        return sellsValue.subtract(buysValue);
    }
}
