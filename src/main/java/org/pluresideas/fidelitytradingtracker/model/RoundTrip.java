package org.pluresideas.fidelitytradingtracker.model;

public class RoundTrip {

    private final String symbol;
    private String openDate;
    private String closeDate = "OPEN";

    private double totalBuyQty = 0.0;
    private double totalSellQty = 0.0;
    private double totalBuyValue = 0.0;
    private double totalSellValue = 0.0;
    private double realizedPnL = 0.0;
    private boolean isClosed = false;
    private boolean isEstimated = false;

    public RoundTrip(String symbol) {
        this.symbol = symbol;
    }

    public void addBuy(Transaction t) {
        if (openDate == null) {
            openDate = t.date();
        }
        totalBuyQty += t.quantity();
        totalBuyValue += Math.abs(t.amount());
    }

    public void addSell(Transaction t) {
        totalSellQty += t.quantity();
        totalSellValue += Math.abs(t.amount());
    }

    public void addPnL(double pnl) {
        realizedPnL += pnl;
    }

    public void setEstimated(boolean estimated) {
        this.isEstimated = estimated;
    }

    public void close(String closeDate) {
        this.closeDate = closeDate;
        this.isClosed = true;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getOpenDate() {
        return openDate;
    }

    public String getCloseDate() {
        return closeDate;
    }

    public double getTotalBuyQty() {
        return totalBuyQty;
    }

    public double getTotalSellQty() {
        return totalSellQty;
    }

    public double getTotalBuyValue() {
        return totalBuyValue;
    }

    public double getTotalSellValue() {
        return totalSellValue;
    }

    public double getRealizedPnL() {
        return realizedPnL;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isEstimated() {
        return isEstimated;
    }

    public double getAvgBuyPrice() {
        return totalBuyQty > 0 ? totalBuyValue / totalBuyQty : 0.0;
    }

    public double getAvgSellPrice() {
        return totalSellQty > 0 ? totalSellValue / totalSellQty : 0.0;
    }
}
