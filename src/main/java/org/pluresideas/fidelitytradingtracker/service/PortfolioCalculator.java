package org.pluresideas.fidelitytradingtracker.service;

import org.pluresideas.fidelitytradingtracker.model.Account;
import org.pluresideas.fidelitytradingtracker.model.Action;
import org.pluresideas.fidelitytradingtracker.model.RoundTrip;
import org.pluresideas.fidelitytradingtracker.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioCalculator {

    public CalculationResults calculate(List<Transaction> transactions) {
        Map<String, Double> symbolSharesOwned = new HashMap<>();
        Map<String, Double> symbolAvgCostBasis = new HashMap<>();
        Map<String, Double> symbolRealizedPnL = new HashMap<>();

        Map<String, Double> symbolTotalSellQty = new HashMap<>();

        Map<String, Account> accounts = new HashMap<>();

        double totalBuys = 0;
        double totalSells = 0;
        int totalBuysCount = 0;
        int totalSellsCount = 0;
        int winningSellsCount = 0;
        boolean hasIncompleteHistory = false;

        List<RoundTrip> allRoundTrips = new ArrayList<>();
        Map<String, RoundTrip> activeRoundTrips = new HashMap<>();

        for (Transaction t : transactions) {
            String sym = t.symbol();
            double qty = t.quantity();
            double amt = Math.abs(t.amount());

            Account acc = accounts.get(t.account());
            if (acc == null) {
                acc = new Account(t.account(), t.accountNum());
                accounts.put(t.account(), acc);
            }
            acc.addTrade(t.action(), amt);

            if (t.action() == Action.BUY) {
                totalBuys += amt;
                totalBuysCount++;

                double currentShares = symbolSharesOwned.getOrDefault(sym, 0.0);
                double currentAvgCost = symbolAvgCostBasis.getOrDefault(sym, 0.0);
                double newShares = currentShares + qty;

                double newAvgCost = 0.0;
                if (newShares > 0) {
                    newAvgCost = ((currentShares * currentAvgCost) + amt) / newShares;
                }
                symbolSharesOwned.put(sym, newShares);
                symbolAvgCostBasis.put(sym, newAvgCost);

                // RoundTrip logic
                RoundTrip rt = activeRoundTrips.get(sym);
                if (rt == null) {
                    rt = new RoundTrip(sym);
                    activeRoundTrips.put(sym, rt);
                    allRoundTrips.add(rt);
                }
                rt.addBuy(t);
            } else {
                totalSells += amt;
                totalSellsCount++;

                symbolTotalSellQty.put(sym, symbolTotalSellQty.getOrDefault(sym, 0.0) + qty);

                double currentShares = symbolSharesOwned.getOrDefault(sym, 0.0);
                double currentAvgCost = symbolAvgCostBasis.getOrDefault(sym, 0.0);

                double costBasis = 0.0;
                boolean incomplete = false;
                if (currentShares > 0) {
                    double sharesFromHolding = Math.min(qty, currentShares);
                    costBasis += sharesFromHolding * currentAvgCost;
                    if (qty > currentShares) {
                        costBasis += (qty - currentShares) * t.price();
                        incomplete = true;
                    }
                } else {
                    costBasis += qty * t.price();
                    incomplete = true;
                }

                if (incomplete) {
                    hasIncompleteHistory = true;
                }

                double pnl = amt - costBasis;
                symbolRealizedPnL.put(sym, symbolRealizedPnL.getOrDefault(sym, 0.0) + pnl);
                if (pnl > 0.0) {
                    winningSellsCount++;
                }

                double newShares = Math.max(0.0, currentShares - qty);
                if (newShares <= 1e-6) {
                    newShares = 0.0;
                    symbolAvgCostBasis.put(sym, 0.0);
                }
                symbolSharesOwned.put(sym, newShares);

                // RoundTrip logic
                RoundTrip rt = activeRoundTrips.get(sym);
                if (rt == null) {
                    rt = new RoundTrip(sym);
                    activeRoundTrips.put(sym, rt);
                    allRoundTrips.add(rt);
                    rt.setEstimated(true);
                }
                rt.addSell(t);
                rt.addPnL(pnl);
                if (incomplete) {
                    rt.setEstimated(true);
                }

                if (newShares == 0.0) {
                    rt.close(t.date());
                    activeRoundTrips.remove(sym);
                }
            }
        }

        double totalRealizedPnL = 0.0;
        int winningSymbolsCount = 0;
        int closedSymbolsCount = 0;
        for (Map.Entry<String, Double> entry : symbolRealizedPnL.entrySet()) {
            totalRealizedPnL += entry.getValue();
            String sym = entry.getKey();
            double sellQty = symbolTotalSellQty.getOrDefault(sym, 0.0);
            if (sellQty > 0) {
                closedSymbolsCount++;
                if (entry.getValue() > 0.0) {
                    winningSymbolsCount++;
                }
            }
        }

        return new CalculationResults(
                transactions, allRoundTrips, accounts, hasIncompleteHistory,
                totalBuys, totalSells, totalBuysCount, totalSellsCount, winningSellsCount,
                totalRealizedPnL, winningSymbolsCount, closedSymbolsCount
        );
    }
}
