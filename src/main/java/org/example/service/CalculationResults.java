package org.example.service;

import org.example.model.Account;
import org.example.model.RoundTrip;
import org.example.model.Transaction;
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
) {}
