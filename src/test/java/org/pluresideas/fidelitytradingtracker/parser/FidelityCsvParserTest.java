package org.pluresideas.fidelitytradingtracker.parser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pluresideas.fidelitytradingtracker.model.Action;
import org.pluresideas.fidelitytradingtracker.model.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        // Col 0: Run Date, Col 1: Account, Col 2: Account Num, Col 3: Action, Col 4: Symbol, Col 5: Description, Col 6: Type, Col 7: Price, Col 8: Quantity, Col 9: Commission, Col 10: Fees, Col 11: Interest, Col 12: Amount
        String content = """
                Run Date,Account,Account #,Action,Symbol,Description,Type,Price,Quantity,Commission,Fees,Interest,Amount
                05/01/2026,Individual,A123,YOU BOUGHT AAPL,AAPL,APPLE INC,Cash,150.00,10.0,"","","",-1500.00
                05/01/2026,Individual,A123,YOU SOLD AAPL,AAPL,APPLE INC,Cash,160.00,-10.0,"","","",1600.00
                """;

        Files.writeString(tempFile, content);

        List<Transaction> transactions = parser.parse(tempFile.toString(), Collections.emptySet());

        assertEquals(2, transactions.size());

        // Verifying chronological sorting and intraday alignment (BUY before SELL)
        Transaction first = transactions.get(0);
        Transaction second = transactions.get(1);

        assertEquals("05/01/2026", first.date());
        assertEquals(Action.BUY, first.action());
        assertEquals("AAPL", first.symbol());
        assertEquals(150.0, first.price());
        assertEquals(10.0, first.quantity());
        assertEquals(-1500.0, first.amount());

        assertEquals("05/01/2026", second.date());
        assertEquals(Action.SELL, second.action());
        assertEquals("AAPL", second.symbol());
        assertEquals(160.0, second.price());
        assertEquals(10.0, second.quantity()); // quantity is absolute value
        assertEquals(1600.0, second.amount());
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
