package com.pluresideas.fidelitytradingtracker.service;

import com.pluresideas.fidelitytradingtracker.model.Account;
import com.pluresideas.fidelitytradingtracker.model.Action;
import com.pluresideas.fidelitytradingtracker.model.RoundTrip;
import com.pluresideas.fidelitytradingtracker.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        transactions.add(new Transaction("05-01-2026", "Individual Account", "A123", Action.BUY, "AAPL", 150.0, 10.0, -1500.0));
        // Sell 10 shares of AAPL at $160 (amount = $1600)
        transactions.add(new Transaction("05-02-2026", "Individual Account", "A123", Action.SELL, "AAPL", 160.0, 10.0, 1600.0));

        CalculationResults results = calculator.calculate(transactions);

        assertEquals(100.0, results.totalRealizedPnL(), 0.001);
        assertFalse(results.hasIncompleteHistory());
        assertEquals(1, results.closedSymbolsCount());
        assertEquals(1, results.winningSymbolsCount());

        List<RoundTrip> roundTrips = results.roundTrips();
        assertEquals(1, roundTrips.size());
        RoundTrip rt = roundTrips.get(0);
        assertEquals("AAPL", rt.getSymbol());
        assertTrue(rt.isClosed());
        assertEquals(10.0, rt.getTotalBuyQty());
        assertEquals(10.0, rt.getTotalSellQty());
        assertEquals(1500.0, rt.getTotalBuyValue());
        assertEquals(1600.0, rt.getTotalSellValue());
        assertEquals(100.0, rt.getRealizedPnL());
        assertFalse(rt.isEstimated());
    }

    @Test
    void testAverageCostBasisMultipleBuys() {
        // Buy 10 shares of AAPL at $150 (amount = -$1500)
        transactions.add(new Transaction("05-01-2026", "Individual Account", "A123", Action.BUY, "AAPL", 150.0, 10.0, -1500.0));
        // Buy 10 shares of AAPL at $170 (amount = -$1700)
        transactions.add(new Transaction("05-02-2026", "Individual Account", "A123", Action.BUY, "AAPL", 170.0, 10.0, -1700.0));
        // Sell 15 shares of AAPL at $180 (amount = $2700)
        // Average cost = (1500 + 1700) / 20 = $160 per share
        // Cost basis for 15 shares = 15 * 160 = $2400
        // Realized PnL = 2700 - 2400 = $300
        transactions.add(new Transaction("05-03-2026", "Individual Account", "A123", Action.SELL, "AAPL", 180.0, 15.0, 2700.0));

        CalculationResults results = calculator.calculate(transactions);

        assertEquals(300.0, results.totalRealizedPnL(), 0.001);
        assertFalse(results.hasIncompleteHistory());

        List<RoundTrip> roundTrips = results.roundTrips();
        assertEquals(1, roundTrips.size());
        RoundTrip rt = roundTrips.get(0);
        assertFalse(rt.isClosed()); // Only partially sold (5 shares still open)
        assertEquals(20.0, rt.getTotalBuyQty());
        assertEquals(15.0, rt.getTotalSellQty());
        assertEquals(300.0, rt.getRealizedPnL());
    }

    @Test
    void testIncompleteHistoryEstimatedPnL() {
        // Sell 10 shares of AAPL at $160 without any previous buys
        transactions.add(new Transaction("05-01-2026", "Individual Account", "A123", Action.SELL, "AAPL", 160.0, 10.0, 1600.0));

        CalculationResults results = calculator.calculate(transactions);

        assertTrue(results.hasIncompleteHistory());
        assertEquals(0.0, results.totalRealizedPnL(), 0.001); // Realized P&L is estimated as $0 since cost basis matches selling price

        List<RoundTrip> roundTrips = results.roundTrips();
        assertEquals(1, roundTrips.size());
        RoundTrip rt = roundTrips.get(0);
        assertTrue(rt.isEstimated());
    }

    @Test
    void testAccountAggregation() {
        transactions.add(new Transaction("05-01-2026", "Account One", "111", Action.BUY, "AAPL", 150.0, 10.0, -1500.0));
        transactions.add(new Transaction("05-01-2026", "Account Two", "222", Action.BUY, "MSFT", 300.0, 5.0, -1500.0));

        CalculationResults results = calculator.calculate(transactions);

        assertEquals(2, results.accounts().size());
        Account acc1 = results.accounts().get("Account One");
        Account acc2 = results.accounts().get("Account Two");

        assertNotNull(acc1);
        assertNotNull(acc2);
        assertEquals("111", acc1.getAccountNumber());
        assertEquals("222", acc2.getAccountNumber());
        assertEquals(1500.0, acc1.getBuysValue());
        assertEquals(1500.0, acc2.getBuysValue());
        assertEquals(1, acc1.getTradesCount());
        assertEquals(1, acc2.getTradesCount());
    }
}
