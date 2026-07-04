package org.pluresideas.fidelitytradingtracker.service;

import org.pluresideas.fidelitytradingtracker.model.Account;
import org.pluresideas.fidelitytradingtracker.model.RoundTrip;
import org.pluresideas.fidelitytradingtracker.model.Transaction;

import java.util.List;
import java.util.Map;

public record CalculationResults(
        List<Transaction> transactions,
        List<RoundTrip> roundTrips,
        Map<String, Account> accounts,
        boolean hasIncompleteHistory,
        double totalBuys,
        double totalSells,
        int totalBuysCount,
        int totalSellsCount,
        int winningSellsCount,
        double totalRealizedPnL,
        int winningSymbolsCount,
        int closedSymbolsCount
) {
}
