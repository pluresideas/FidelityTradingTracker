package org.pluresideas.fidelitytradingtracker.model;

public class Account {
    private final String name;
    private final String accountNumber;
    private int tradesCount;
    private double buysValue;
    private double sellsValue;

    public Account(String name, String accountNumber) {
        this.name = name;
        this.accountNumber = accountNumber;
    }

    public void addTrade(Action action, double amount) {
        tradesCount++;
        if (action == Action.BUY) {
            buysValue += amount;
        } else if (action == Action.SELL) {
            sellsValue += amount;
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

    public double getBuysValue() {
        return buysValue;
    }

    public double getSellsValue() {
        return sellsValue;
    }

    public double getNetCashFlow() {
        return sellsValue - buysValue;
    }
}
