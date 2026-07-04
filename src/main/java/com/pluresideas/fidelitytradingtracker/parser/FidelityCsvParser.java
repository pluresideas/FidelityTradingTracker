package com.pluresideas.fidelitytradingtracker.parser;

import com.pluresideas.fidelitytradingtracker.model.Action;
import com.pluresideas.fidelitytradingtracker.model.Transaction;

import java.io.IOException;
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

            double price = parseDoubleSafely(fields.get(7));
            double quantity = Math.abs(parseDoubleSafely(fields.get(8)));
            double amount = parseDoubleSafely(fields.get(12));

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

        // Sort transactions chronologically (oldest first). For transactions on the same day, sort BUY before SELL.
        transactions.sort((t1, t2) -> {
            String d1 = getComparableDate(t1.date());
            String d2 = getComparableDate(t2.date());
            int dateComp = d1.compareTo(d2);
            if (dateComp != 0) {
                return dateComp;
            }
            if (t1.action() == t2.action()) {
                return 0;
            }
            return t1.action() == Action.BUY ? -1 : 1;
        });

        return transactions;
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

    private double parseDoubleSafely(String val) {
        if (val == null || val.trim().isEmpty() || val.equals("\"\"")) {
            return 0.0;
        }
        try {
            String cleanVal = val.replace("$", "").replace(",", "").replace("\"", "").trim();
            return Double.parseDouble(cleanVal);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String getComparableDate(String dateStr) {
        String[] parts = dateStr.split("-");
        if (parts.length == 3) {
            return parts[2] + parts[0] + parts[1]; // YYYYMMDD
        }
        return dateStr;
    }
}
