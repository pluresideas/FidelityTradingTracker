package com.pluresideas.fidelitytradingtracker.parser;

import com.pluresideas.fidelitytradingtracker.model.Transaction;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface CsvParser {
    List<Transaction> parse(String filePath, Set<String> ignoredSymbols) throws IOException;
}
