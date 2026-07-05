package com.pluresideas.fidelitytradingtracker.model;

import java.math.BigDecimal;
import java.math.MathContext;

public class RoundTrip {

    private final String symbol;
    private String openDate;
    private String closeDate = "OPEN";

    private BigDecimal totalBuyQty = BigDecimal.ZERO;
    private BigDecimal totalSellQty = BigDecimal.ZERO;
    private BigDecimal totalBuyValue = BigDecimal.ZERO;
    private BigDecimal totalSellValue = BigDecimal.ZERO;
    private BigDecimal realizedPnL = BigDecimal.ZERO;
    private boolean isClosed = false;
    private boolean isEstimated = false;

    public RoundTrip(String symbol) {
        this.symbol = symbol;
    }

    public void addBuy(Transaction t) {
        if (openDate == null) {
            openDate = t.date();
        }
        totalBuyQty = totalBuyQty.add(t.quantity());
        totalBuyValue = totalBuyValue.add(t.amount().abs());
    }

    public void addSell(Transaction t) {
        addSell(t.quantity(), t.price());
    }

    public void addSell(BigDecimal qty, BigDecimal price) {
        totalSellQty = totalSellQty.add(qty);
        totalSellValue = totalSellValue.add(qty.multiply(price));
    }

    public void addPnL(BigDecimal pnl) {
        realizedPnL = realizedPnL.add(pnl);
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

    public BigDecimal getTotalBuyQty() {
        return totalBuyQty;
    }

    public BigDecimal getTotalSellQty() {
        return totalSellQty;
    }

    public BigDecimal getTotalBuyValue() {
        return totalBuyValue;
    }

    public BigDecimal getTotalSellValue() {
        return totalSellValue;
    }

    public BigDecimal getRealizedPnL() {
        return realizedPnL;
    }

    public BigDecimal getReturnPercentage() {
        if (totalBuyValue == null || totalBuyValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return realizedPnL.multiply(new BigDecimal("100"))
                .divide(totalBuyValue, MathContext.DECIMAL128);
    }

    public boolean isClosed() {
        return isClosed;
    }

    public boolean isEstimated() {
        return isEstimated;
    }

    public BigDecimal getAvgBuyPrice() {
        return totalBuyQty.compareTo(BigDecimal.ZERO) > 0 ? totalBuyValue.divide(totalBuyQty, MathContext.DECIMAL128) : BigDecimal.ZERO;
    }

    public BigDecimal getAvgSellPrice() {
        return totalSellQty.compareTo(BigDecimal.ZERO) > 0 ? totalSellValue.divide(totalSellQty, MathContext.DECIMAL128) : BigDecimal.ZERO;
    }
}
