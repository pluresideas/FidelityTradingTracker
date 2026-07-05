package com.pluresideas.fidelitytradingtracker.service;

import com.pluresideas.fidelitytradingtracker.model.Account;
import com.pluresideas.fidelitytradingtracker.model.Action;
import com.pluresideas.fidelitytradingtracker.model.RoundTrip;
import com.pluresideas.fidelitytradingtracker.model.Transaction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioCalculator {

    public CalculationResults calculate(List<Transaction> transactions) {
        Map<String, BigDecimal> symbolSharesOwned = new HashMap<>();
        Map<String, BigDecimal> symbolAvgCostBasis = new HashMap<>();
        Map<String, BigDecimal> symbolRealizedPnL = new HashMap<>();
        Map<String, BigDecimal> symbolTotalSellQty = new HashMap<>();

        Map<String, Account> accounts = new HashMap<>();

        BigDecimal totalBuys = BigDecimal.ZERO;
        BigDecimal totalSells = BigDecimal.ZERO;
        int totalBuysCount = 0;
        int totalSellsCount = 0;
        int winningSellsCount = 0;
        boolean hasIncompleteHistory = false;

        List<RoundTrip> allRoundTrips = new ArrayList<>();
        Map<String, RoundTrip> activeRoundTrips = new HashMap<>();

        for (Transaction t : transactions) {
            String sym = t.symbol();
            BigDecimal qty = t.quantity();
            BigDecimal amt = t.amount().abs();

            Account acc = accounts.get(t.account());
            if (acc == null) {
                acc = new Account(t.account(), t.accountNum());
                accounts.put(t.account(), acc);
            }
            acc.addTrade(t.action(), amt);

            if (t.action() == Action.BUY) {
                totalBuys = totalBuys.add(amt);
                totalBuysCount++;

                BigDecimal currentShares = symbolSharesOwned.getOrDefault(sym, BigDecimal.ZERO);
                BigDecimal currentAvgCost = symbolAvgCostBasis.getOrDefault(sym, BigDecimal.ZERO);
                BigDecimal newShares = currentShares.add(qty);

                BigDecimal newAvgCost = BigDecimal.ZERO;
                if (newShares.compareTo(BigDecimal.ZERO) > 0) {
                    newAvgCost = currentShares.multiply(currentAvgCost)
                            .add(amt)
                            .divide(newShares, MathContext.DECIMAL128);
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
                totalSells = totalSells.add(amt);
                totalSellsCount++;

                symbolTotalSellQty.put(sym, symbolTotalSellQty.getOrDefault(sym, BigDecimal.ZERO).add(qty));

                BigDecimal currentShares = symbolSharesOwned.getOrDefault(sym, BigDecimal.ZERO);
                BigDecimal currentAvgCost = symbolAvgCostBasis.getOrDefault(sym, BigDecimal.ZERO);

                BigDecimal costBasis = BigDecimal.ZERO;
                boolean incomplete = false;
                if (currentShares.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sharesFromHolding = qty.min(currentShares);
                    costBasis = costBasis.add(sharesFromHolding.multiply(currentAvgCost));
                    if (qty.compareTo(currentShares) > 0) {
                        BigDecimal missingQty = qty.subtract(currentShares);
                        costBasis = costBasis.add(missingQty.multiply(t.price()));
                        incomplete = true;
                    }
                } else {
                    costBasis = costBasis.add(qty.multiply(t.price()));
                    incomplete = true;
                }

                if (incomplete) {
                    hasIncompleteHistory = true;
                }

                BigDecimal pnl = amt.subtract(costBasis);
                symbolRealizedPnL.put(sym, symbolRealizedPnL.getOrDefault(sym, BigDecimal.ZERO).add(pnl));
                if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                    winningSellsCount++;
                }

                BigDecimal newShares = currentShares.subtract(qty);
                if (newShares.compareTo(BigDecimal.ZERO) <= 0) {
                    newShares = BigDecimal.ZERO;
                    symbolAvgCostBasis.put(sym, BigDecimal.ZERO);
                }
                symbolSharesOwned.put(sym, newShares);

                // RoundTrip logic
                RoundTrip rt = activeRoundTrips.get(sym);
                if (rt != null) {
                    BigDecimal sellQtyForRoundTrip = qty;
                    BigDecimal pnlForRoundTrip = pnl;
                    if (qty.compareTo(currentShares) > 0) {
                        sellQtyForRoundTrip = currentShares;
                        pnlForRoundTrip = currentShares.multiply(t.price())
                                .subtract(currentShares.multiply(currentAvgCost));
                        rt.setEstimated(true);
                    }
                    rt.addSell(sellQtyForRoundTrip, t.price());
                    rt.addPnL(pnlForRoundTrip);
                    if (incomplete) {
                        rt.setEstimated(true);
                    }
                    if (newShares.compareTo(BigDecimal.ZERO) == 0) {
                        rt.close(t.date());
                        activeRoundTrips.remove(sym);
                    }
                }
            }
        }

        BigDecimal totalRealizedPnL = BigDecimal.ZERO;
        int winningSymbolsCount = 0;
        int closedSymbolsCount = 0;
        for (Map.Entry<String, BigDecimal> entry : symbolRealizedPnL.entrySet()) {
            totalRealizedPnL = totalRealizedPnL.add(entry.getValue());
            String sym = entry.getKey();
            BigDecimal sellQty = symbolTotalSellQty.getOrDefault(sym, BigDecimal.ZERO);
            if (sellQty.compareTo(BigDecimal.ZERO) > 0) {
                closedSymbolsCount++;
                if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
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
