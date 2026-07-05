package com.pluresideas.fidelitytradingtracker.parser;

import com.pluresideas.fidelitytradingtracker.model.Action;
import com.pluresideas.fidelitytradingtracker.model.Transaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FidelityCsvParser implements CsvParser {

    @Override
    public List<Transaction> parse(String filePath, Set<String> ignoredSymbols) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        List<Transaction> transactions = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> fields = parseCsvLine(line);
            if (fields.isEmpty()) {
                continue;
            }
            String dateField = fields.get(0);
            if (dateField.isEmpty() || !Character.isDigit(dateField.charAt(0))) {
                continue;
            }
            if (fields.size() < 13) {
                continue;
            }

            String action = fields.get(3);
            boolean isBuy = action.toUpperCase().contains("YOU BOUGHT");
            boolean isSell = action.toUpperCase().contains("YOU SOLD");
            if (!isBuy && !isSell) {
                continue;
            }

            String symbol = fields.get(4).trim();
            if (ignoredSymbols.contains(symbol.toUpperCase())) {
                continue;
            }

            BigDecimal price = parseBigDecimalSafely(fields.get(7));
            BigDecimal quantity = parseBigDecimalSafely(fields.get(8)).abs();
            BigDecimal amount = parseBigDecimalSafely(fields.get(12));

            Transaction tx = new Transaction(
                    dateField,
                    fields.get(1),
                    fields.get(2),
                    isBuy ? Action.BUY : Action.SELL,
                    symbol,
                    price,
                    quantity,
                    amount
            );
            transactions.add(tx);
        }

        // Reversing the parsed list because Fidelity CSV exports are always in reverse chronological order (newest first).
        // Reversing naturally puts them in correct chronological order (oldest first) while preserving transaction blocks.
        java.util.Collections.reverse(transactions);

        // Group contiguous transactions of the same date and sort each group to prioritize BUY before SELL.
        List<Transaction> sortedTransactions = new ArrayList<>();
        int i = 0;
        int n = transactions.size();
        while (i < n) {
            String currentDate = transactions.get(i).date();
            List<Transaction> group = new ArrayList<>();
            while (i < n && transactions.get(i).date().equals(currentDate)) {
                group.add(transactions.get(i));
                i++;
            }
            // Sort this day group: BUY before SELL
            group.sort((t1, t2) -> {
                if (t1.action() == t2.action()) {
                    return 0;
                }
                return t1.action() == Action.BUY ? -1 : 1;
            });
            sortedTransactions.addAll(group);
        }
        return sortedTransactions;
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result;
    }

    private BigDecimal parseBigDecimalSafely(String val) {
        if (val == null || val.trim().isEmpty() || val.equals("\"\"")) {
            return BigDecimal.ZERO;
        }
        try {
            String cleanVal = val.replace("$", "").replace(",", "").replace("\"", "").trim();
            return new BigDecimal(cleanVal);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
