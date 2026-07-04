package org.example;

import org.example.model.Transaction;
import org.example.parser.CsvParser;
import org.example.parser.FidelityCsvParser;
import org.example.report.FidelityTrackerReport;
import org.example.report.Report;
import org.example.service.CalculationResults;
import org.example.service.PortfolioCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        if (args.length < 1) {
            printUsage();
            return;
        }

        String inputPath = args[0];
        Set<String> ignoredSymbols = args.length >= 2 ? parseIgnoredSymbols(args[1]) : new HashSet<>();

        List<Transaction> transactions = parseTransactions(inputPath, ignoredSymbols);
        if (transactions.isEmpty()) {
            return;
        }

        CalculationResults results = calculateResults(transactions);
        renderReport(inputPath, results);
    }

    private static void printUsage() {
        logger.error("Error: Missing mandatory arguments.");
        logger.info("Usage: java -cp ... org.example.Main <path-to-accounts-history-csv> [<path-to-ignore-symbols-txt>]");
        logger.info("Arguments:");
        logger.info("  <path-to-accounts-history-csv> : Path to the exported Fidelity transaction history CSV file containing your trade logs.");
        logger.info("  [<path-to-ignore-symbols-txt>]  : (Optional) Path to a plain text file containing stock symbols to ignore (one ticker per line, e.g. SPAXX).");
    }

    private static Set<String> parseIgnoredSymbols(String ignoreInputPath) {

        Set<String> ignoredSymbols = new HashSet<>();
        try {
            final Path path = Paths.get(ignoreInputPath);
            if (Files.exists(path)) {
                List<String> ignoreLines = Files.readAllLines(path);
                for (String line : ignoreLines) {
                    if (!line.trim().isEmpty()) {
                        ignoredSymbols.add(line.trim().toUpperCase());
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Warning: Error reading ignore file {}: {}", ignoreInputPath, e.getMessage());
        }
        return ignoredSymbols;
    }

    private static List<Transaction> parseTransactions(String inputPath, Set<String> ignoredSymbols) {
        CsvParser parser = new FidelityCsvParser();
        try {
            return parser.parse(inputPath, ignoredSymbols);
        } catch (IOException e) {
            logger.error("Error reading the file: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static CalculationResults calculateResults(List<Transaction> transactions) {
        PortfolioCalculator calculator = new PortfolioCalculator();
        return calculator.calculate(transactions);
    }

    private static void renderReport(String inputPath, CalculationResults results) {
        Report report = new FidelityTrackerReport(inputPath);
        report.render(results);
    }
}
