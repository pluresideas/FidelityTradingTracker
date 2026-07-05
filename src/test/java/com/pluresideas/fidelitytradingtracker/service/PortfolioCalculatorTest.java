package com.pluresideas.fidelitytradingtracker.service;

import com.pluresideas.fidelitytradingtracker.model.Account;
import com.pluresideas.fidelitytradingtracker.model.Action;
import com.pluresideas.fidelitytradingtracker.model.RoundTrip;
import com.pluresideas.fidelitytradingtracker.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioCalculatorTest {

    private PortfolioCalculator calculator;
    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        calculator = new PortfolioCalculator();
        transactions = new ArrayList<>();
    }

    @Test
    void testSingleBuyAndSell() {
        // Buy 10 shares of AAPL at $150 (amount = -$1500)
        transactions.add(new Transaction("05-01-2026", "Individual Account", "A123", Action.BUY, "AAPL", new BigDecimal("150.0"), new BigDecimal("10.0"), new BigDecimal("-1500.0")));
        // Sell 10 shares of AAPL at $160 (amount = $1600)
        transactions.add(new Transaction("05-02-2026", "Individual Account", "A123", Action.SELL, "AAPL", new BigDecimal("160.0"), new BigDecimal("10.0"), new BigDecimal("1600.0")));

        CalculationResults results = calculator.calculate(transactions);

        assertEquals(100.0, results.totalRealizedPnL().doubleValue(), 0.001);
        assertFalse(results.hasIncompleteHistory());
        assertEquals(1, results.closedSymbolsCount());
        assertEquals(1, results.winningSymbolsCount());

        List<RoundTrip> roundTrips = results.roundTrips();
        assertEquals(1, roundTrips.size());
        RoundTrip rt = roundTrips.get(0);
        assertEquals("AAPL", rt.getSymbol());
        assertTrue(rt.isClosed());
        assertEquals(10.0, rt.getTotalBuyQty().doubleValue(), 0.001);
        assertEquals(10.0, rt.getTotalSellQty().doubleValue(), 0.001);
        assertEquals(1500.0, rt.getTotalBuyValue().doubleValue(), 0.001);
        assertEquals(1600.0, rt.getTotalSellValue().doubleValue(), 0.001);
        assertEquals(100.0, rt.getRealizedPnL().doubleValue(), 0.001);
        assertFalse(rt.isEstimated());
    }

    @Test
    void testAverageCostBasisMultipleBuys() {
        // Buy 10 shares of AAPL at $150 (amount = -$1500)
        transactions.add(new Transaction("05-01-2026", "Individual Account", "A123", Action.BUY, "AAPL", new BigDecimal("150.0"), new BigDecimal("10.0"), new BigDecimal("-1500.0")));
        // Buy 10 shares of AAPL at $170 (amount = -$1700)
        transactions.add(new Transaction("05-02-2026", "Individual Account", "A123", Action.BUY, "AAPL", new BigDecimal("170.0"), new BigDecimal("10.0"), new BigDecimal("-1700.0")));
        // Sell 15 shares of AAPL at $180 (amount = $2700)
        // Average cost = (1500 + 1700) / 20 = $160 per share
        // Cost basis for 15 shares = 15 * 160 = $2400
        // Realized PnL = 2700 - 2400 = $300
        transactions.add(new Transaction("05-03-2026", "Individual Account", "A123", Action.SELL, "AAPL", new BigDecimal("180.0"), new BigDecimal("15.0"), new BigDecimal("2700.0")));

        CalculationResults results = calculator.calculate(transactions);

        assertEquals(300.0, results.totalRealizedPnL().doubleValue(), 0.001);
        assertFalse(results.hasIncompleteHistory());

        List<RoundTrip> roundTrips = results.roundTrips();
        assertEquals(1, roundTrips.size());
        RoundTrip rt = roundTrips.get(0);
        assertFalse(rt.isClosed()); // Only partially sold (5 shares still open)
        assertEquals(20.0, rt.getTotalBuyQty().doubleValue(), 0.001);
        assertEquals(15.0, rt.getTotalSellQty().doubleValue(), 0.001);
        assertEquals(300.0, rt.getRealizedPnL().doubleValue(), 0.001);
    }

    @Test
    void testIncompleteHistoryEstimatedPnL() {
        // Sell 10 shares of AAPL at $160 without any previous buys (starts with sell, no transaction should be created)
        transactions.add(new Transaction("05-01-2026", "Individual Account", "A123", Action.SELL, "AAPL", new BigDecimal("160.0"), new BigDecimal("10.0"), new BigDecimal("1600.0")));

        CalculationResults results = calculator.calculate(transactions);

        assertTrue(results.hasIncompleteHistory());
        assertEquals(0.0, results.totalRealizedPnL().doubleValue(), 0.001);

        List<RoundTrip> roundTrips = results.roundTrips();
        assertTrue(roundTrips.isEmpty()); // Cannot start a transaction with a sell
    }

    @Test
    void testIncompleteHistoryPartialMissingBuys() {
        // Buy 5 shares at $100
        transactions.add(new Transaction("05-01-2026", "Individual Account", "A123", Action.BUY, "AAPL", new BigDecimal("100.0"), new BigDecimal("5.0"), new BigDecimal("-500.0")));
        // Sell 10 shares at $120 (5 shares have buy history, 5 shares do not)
        transactions.add(new Transaction("05-02-2026", "Individual Account", "A123", Action.SELL, "AAPL", new BigDecimal("120.0"), new BigDecimal("10.0"), new BigDecimal("1200.0")));

        CalculationResults results = calculator.calculate(transactions);

        assertTrue(results.hasIncompleteHistory());
        // 5 shares sold at 120 with cost basis 100 -> P&L = +100
        // 5 shares sold at 120 with no cost basis -> P&L = +0
        assertEquals(100.0, results.totalRealizedPnL().doubleValue(), 0.001);

        List<RoundTrip> roundTrips = results.roundTrips();
        assertEquals(1, roundTrips.size());
        RoundTrip rt = roundTrips.get(0);
        assertTrue(rt.isClosed());
        assertTrue(rt.isEstimated());
        assertEquals(5.0, rt.getTotalBuyQty().doubleValue(), 0.001);
        assertEquals(5.0, rt.getTotalSellQty().doubleValue(), 0.001); // capped at owned buy qty
        assertEquals(100.0, rt.getRealizedPnL().doubleValue(), 0.001);
    }

    @Test
    void testAccountAggregation() {
        transactions.add(new Transaction("05-01-2026", "Account One", "111", Action.BUY, "AAPL", new BigDecimal("150.0"), new BigDecimal("10.0"), new BigDecimal("-1500.0")));
        transactions.add(new Transaction("05-01-2026", "Account Two", "222", Action.BUY, "MSFT", new BigDecimal("300.0"), new BigDecimal("5.0"), new BigDecimal("-1500.0")));

        CalculationResults results = calculator.calculate(transactions);

        assertEquals(2, results.accounts().size());
        Account acc1 = results.accounts().get("Account One");
        Account acc2 = results.accounts().get("Account Two");

        assertNotNull(acc1);
        assertNotNull(acc2);
        assertEquals("111", acc1.getAccountNumber());
        assertEquals("222", acc2.getAccountNumber());
        assertEquals(1500.0, acc1.getBuysValue().doubleValue(), 0.001);
        assertEquals(1500.0, acc2.getBuysValue().doubleValue(), 0.001);
        assertEquals(1, acc1.getTradesCount());
        assertEquals(1, acc2.getTradesCount());
    }
}
