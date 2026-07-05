package com.pluresideas.fidelitytradingtracker.parser;

import com.pluresideas.fidelitytradingtracker.model.Action;
import com.pluresideas.fidelitytradingtracker.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FidelityCsvParserTest {

    private Path tempFile;
    private FidelityCsvParser parser;

    @BeforeEach
    void setUp() throws IOException {
        parser = new FidelityCsvParser();
        tempFile = Files.createTempFile("FidelityCsvParserTest", ".csv");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testParseValidTransactions() throws IOException {
        // Headers and raw data representation:
        String content = """
                Run Date,Account,Account #,Action,Symbol,Description,Type,Price,Quantity,Commission,Fees,Interest,Amount
                05/01/2026,Individual,A123,YOU SOLD AAPL,AAPL,APPLE INC,Cash,160.00,-10.0,"","","",1600.00
                05/01/2026,Individual,A123,YOU BOUGHT AAPL,AAPL,APPLE INC,Cash,150.00,10.0,"","","",-1500.00
                """;

        Files.writeString(tempFile, content);

        List<Transaction> transactions = parser.parse(tempFile.toString(), Collections.emptySet());

        assertEquals(2, transactions.size());

        // Verifying chronological sorting (BUY before SELL)
        Transaction first = transactions.get(0);
        Transaction second = transactions.get(1);

        assertEquals("05/01/2026", first.date());
        assertEquals(Action.BUY, first.action());
        assertEquals("AAPL", first.symbol());
        assertEquals(0, first.price().compareTo(new BigDecimal("150.00")));
        assertEquals(0, first.quantity().compareTo(new BigDecimal("10.0")));
        assertEquals(0, first.amount().compareTo(new BigDecimal("-1500.00")));

        assertEquals("05/01/2026", second.date());
        assertEquals(Action.SELL, second.action());
        assertEquals("AAPL", second.symbol());
        assertEquals(0, second.price().compareTo(new BigDecimal("160.00")));
        assertEquals(0, second.quantity().compareTo(new BigDecimal("10.0"))); // quantity is absolute value
        assertEquals(0, second.amount().compareTo(new BigDecimal("1600.00")));
    }

    @Test
    void testIgnoredSymbolsFilter() throws IOException {
        String content = """
                Run Date,Account,Account #,Action,Symbol,Description,Type,Price,Quantity,Commission,Fees,Interest,Amount
                05/01/2026,Individual,A123,YOU BOUGHT SPAXX,SPAXX,FIDELITY GOVERNMENT MONEY MARKET,Cash,1.00,1000.0,"","","",-1000.00
                05/01/2026,Individual,A123,YOU BOUGHT AAPL,AAPL,APPLE INC,Cash,150.00,10.0,"","","",-1500.00
                """;

        Files.writeString(tempFile, content);

        Set<String> ignoreList = new HashSet<>();
        ignoreList.add("SPAXX");

        List<Transaction> transactions = parser.parse(tempFile.toString(), ignoreList);

        assertEquals(1, transactions.size());
        assertEquals("AAPL", transactions.getFirst().symbol());
    }
}
