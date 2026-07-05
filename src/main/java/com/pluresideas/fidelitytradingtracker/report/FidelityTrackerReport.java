package com.pluresideas.fidelitytradingtracker.report;

import com.pluresideas.fidelitytradingtracker.model.Account;
import com.pluresideas.fidelitytradingtracker.model.RoundTrip;
import com.pluresideas.fidelitytradingtracker.service.CalculationResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class FidelityTrackerReport implements Report {
    private static final Logger logger = LoggerFactory.getLogger(FidelityTrackerReport.class);
    public static final String SEPARATOR = "========================================================================================================================";
    private final String inputSource;

    public FidelityTrackerReport(String inputSource) {
        this.inputSource = inputSource;
    }

    @Override
    public void render(CalculationResults r) {
        logger.info(SEPARATOR);
        logger.info("                                            FIDELITY TRADING TRACKER REPORT                                             ");
        logger.info(SEPARATOR);
        logger.info("Data Source: {}", inputSource);
        if (!r.transactions().isEmpty()) {
            logger.info("Report Period: {} to {}", r.transactions().getFirst().date(), r.transactions().getLast().date());
        }
        logger.info("");

        renderOverallSummary(r);
        renderAccountSummary(r);
        renderDetailedSymbolReport(r);
        renderDetailedPnLReport(r);
        renderDetailedDateReport(r);

        if (r.hasIncompleteHistory()) {
            logger.info("  * Note: Realized P&L has limited accuracy (marked with *) due to missing historical purchase data for some sells.");
        }
        logger.info(SEPARATOR);
    }

    private void renderOverallSummary(CalculationResults r) {
        logger.info("[1] OVERALL TRANSACTION SUMMARY");
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info(String.format("  Total Trades:              %d (Buys: %d, Sells: %d)", r.transactions().size(), r.totalBuysCount(), r.totalSellsCount()));
        logger.info(String.format("  Total Cash Inflow:         %s (From Sells)", formatMoney(r.totalSells())));
        logger.info(String.format("  Total Cash Outflow:        %s (To Buys)", formatMoney(r.totalBuys().negate())));
        logger.info(String.format("  Net Cash Flow:             %s", formatMoney(r.totalSells().subtract(r.totalBuys()))));

        double sellWinRate = r.totalSellsCount() > 0 ? ((double) r.winningSellsCount() / r.totalSellsCount() * 100.0) : 0.0;

        int closedRoundTripsCount = 0;
        int winningRoundTripsCount = 0;
        for (RoundTrip rt : r.roundTrips()) {
            if (rt.isClosed()) {
                closedRoundTripsCount++;
                if (rt.getRealizedPnL().compareTo(BigDecimal.ZERO) > 0) {
                    winningRoundTripsCount++;
                }
            }
        }
        double transactionWinRate = closedRoundTripsCount > 0 ? ((double) winningRoundTripsCount / closedRoundTripsCount * 100.0) : 0.0;

        logger.info(String.format("  Total Realized P&L:        %s%s", formatMoney(r.totalRealizedPnL()), r.hasIncompleteHistory() ? "*" : ""));
        logger.info(String.format("  Win Rate (by Transaction): %.1f%% (%d of %d completed transactions)", transactionWinRate, winningRoundTripsCount, closedRoundTripsCount));
        logger.info(String.format("  Win Rate (by Sell):        %.1f%% (%d of %d profitable sells)", sellWinRate, r.winningSellsCount(), r.totalSellsCount()));
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info("");
    }

    private void renderAccountSummary(CalculationResults r) {
        logger.info("[2] ACCOUNT SUMMARY");
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info(String.format("  %-25s %-12s %8s %18s %18s %18s", "Account Name", "Account #", "Trades", "Total Buys", "Total Sells", "Net Cash Flow"));
        logger.info("  ----------------------------------------------------------------------------------------------------------------------");

        List<Account> sortedAccounts = new ArrayList<>(r.accounts().values());
        sortedAccounts.sort(Comparator.comparing(Account::getName));
        for (Account acc : sortedAccounts) {
            logger.info(String.format("  %-25s %-12s %8d %18s %18s %18s",
                acc.getName(),
                acc.getAccountNumber() != null ? acc.getAccountNumber() : "N/A",
                acc.getTradesCount(),
                formatMoney(acc.getBuysValue()),
                formatMoney(acc.getSellsValue()),
                formatMoney(acc.getNetCashFlow())
            ));
        }
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info("");
    }

    private void renderDetailedSymbolReport(CalculationResults r) {
        logger.info("[3] DETAILED TRANSACTIONS BY SYMBOL (Round-Trip Ledger)");
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info(String.format("  %-10s %-10s %-8s %10s %12s %12s %14s %14s %15s",
            "Open Date", "Close Date", "Symbol", "Qty", "Avg Buy", "Avg Sell", "Cost Basis", "Proceeds", "Realized P&L"));
        logger.info("  ----------------------------------------------------------------------------------------------------------------------");

        List<RoundTrip> sortedTransactionsBySymbol = new ArrayList<>(r.roundTrips());
        sortedTransactionsBySymbol.sort((t1, t2) -> {
            int symComp = t1.getSymbol().compareTo(t2.getSymbol());
            if (symComp != 0) {
                return symComp;
            }
            String d1 = getComparableDate(t1.getOpenDate());
            String d2 = getComparableDate(t2.getOpenDate());
            return d1.compareTo(d2);
        });

        for (RoundTrip rt : sortedTransactionsBySymbol) {
            String pnlStr = formatMoney(rt.getRealizedPnL()) + (rt.isEstimated() ? "*" : "");
            String avgSellStr = rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getAvgSellPrice()) : "-";
            String proceedsStr = rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getTotalSellValue()) : "-";

            logger.info(String.format("  %-10s %-10s %-8s %10s %12s %12s %14s %14s %15s",
                rt.getOpenDate() != null ? rt.getOpenDate() : "N/A", rt.getCloseDate(), rt.getSymbol(), formatQty(rt.getTotalBuyQty()),
                formatMoney(rt.getAvgBuyPrice()), avgSellStr, formatMoney(rt.getTotalBuyValue()),
                proceedsStr, pnlStr));
        }
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info("");
    }

    private void renderDetailedPnLReport(CalculationResults r) {
        logger.info("[4] DETAILED TRANSACTIONS BY P&L (Descending)");
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info(String.format("  %-10s %-10s %-8s %10s %12s %12s %14s %14s %15s",
            "Open Date", "Close Date", "Symbol", "Qty", "Avg Buy", "Avg Sell", "Cost Basis", "Proceeds", "Realized P&L"));
        logger.info("  ----------------------------------------------------------------------------------------------------------------------");

        List<RoundTrip> sellTransactionsSortedByPnL = new ArrayList<>();
        for (RoundTrip rt : r.roundTrips()) {
            if (rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0) {
                sellTransactionsSortedByPnL.add(rt);
            }
        }
        sellTransactionsSortedByPnL.sort((t1, t2) -> t2.getRealizedPnL().compareTo(t1.getRealizedPnL()));

        for (RoundTrip rt : sellTransactionsSortedByPnL) {
            String pnlStr = formatMoney(rt.getRealizedPnL()) + (rt.isEstimated() ? "*" : "");
            String avgSellStr = rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getAvgSellPrice()) : "-";
            String proceedsStr = rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getTotalSellValue()) : "-";

            logger.info(String.format("  %-10s %-10s %-8s %10s %12s %12s %14s %14s %15s",
                rt.getOpenDate() != null ? rt.getOpenDate() : "N/A", rt.getCloseDate(), rt.getSymbol(), formatQty(rt.getTotalBuyQty()),
                formatMoney(rt.getAvgBuyPrice()), avgSellStr, formatMoney(rt.getTotalBuyValue()),
                proceedsStr, pnlStr));
        }
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info("");
    }

    private void renderDetailedDateReport(CalculationResults r) {
        logger.info("[5] DETAILED TRANSACTIONS BY DATE (Chronological)");
        logger.info("------------------------------------------------------------------------------------------------------------------------");
        logger.info(String.format("  %-10s %-10s %-8s %10s %12s %12s %14s %14s %15s",
            "Open Date", "Close Date", "Symbol", "Qty", "Avg Buy", "Avg Sell", "Cost Basis", "Proceeds", "Realized P&L"));
        logger.info("  ----------------------------------------------------------------------------------------------------------------------");

        List<RoundTrip> sortedTransactionsByDate = new ArrayList<>(r.roundTrips());
        sortedTransactionsByDate.sort((t1, t2) -> {
            String d1 = getComparableDate(t1.getOpenDate());
            String d2 = getComparableDate(t2.getOpenDate());
            int dateComp = d1.compareTo(d2);
            if (dateComp != 0) {
                return dateComp;
            }
            return t1.getSymbol().compareTo(t2.getSymbol());
        });

        for (RoundTrip rt : sortedTransactionsByDate) {
            String pnlStr = formatMoney(rt.getRealizedPnL()) + (rt.isEstimated() ? "*" : "");
            String avgSellStr = rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getAvgSellPrice()) : "-";
            String proceedsStr = rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getTotalSellValue()) : "-";

            logger.info(String.format("  %-10s %-10s %-8s %10s %12s %12s %14s %14s %15s",
                rt.getOpenDate() != null ? rt.getOpenDate() : "N/A", rt.getCloseDate(), rt.getSymbol(), formatQty(rt.getTotalBuyQty()),
                formatMoney(rt.getAvgBuyPrice()), avgSellStr, formatMoney(rt.getTotalBuyValue()),
                proceedsStr, pnlStr));
        }
        logger.info("------------------------------------------------------------------------------------------------------------------------");
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) {
            return "-";
        }
        BigDecimal rounded = val.setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.ZERO) < 0) {
            return String.format("-$%,.2f", rounded.abs().doubleValue());
        } else {
            return String.format("$%,.2f", rounded.doubleValue());
        }
    }

    private String formatQty(BigDecimal val) {
        if (val == null) {
            return "-";
        }
        DecimalFormat df = new DecimalFormat("#,##0.######");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(val);
    }

    private String getComparableDate(String dateStr) {
        if (dateStr == null) {
            return "";
        }
        String[] parts = dateStr.split("-");
        if (parts.length == 3) {
            return parts[2] + parts[0] + parts[1]; // YYYYMMDD
        }
        return dateStr;
    }
}
