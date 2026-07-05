# Fidelity Trading Tracker

A lightweight, robust Java application that parses Fidelity transaction history exports, computes key portfolio performance metrics using average cost basis, and prints a beautifully structured trading performance report directly to your terminal.

---

## Key Features

- **Standard CSV Parsing Engine**: A custom, zero-dependency CSV state machine parser that gracefully handles empty lines, quoted fields containing spaces or commas, headers, and file footnotes.
- **Chronological Sorting & Intraday Alignment**: Re-orders transactions chronologically and prioritizes intraday **BUY** orders before **SELL** orders to ensure cost basis tracking is accurate and prevents artificial short-selling errors.
- **Average Cost Basis & Realized P&L**: Calculates running average cost basis for open holdings and computes exact realized Profit & Loss for completed trades. If historical buy data is missing, calculations gracefully fall back to estimated values.
- **Exclusion Filter**: Easily filter out cash-equivalent funds (e.g., money markets like `SPAXX`) or index funds (e.g., `FZROX`, `FZILX`) by listing them in a blacklist config file.
- **Comprehensive Terminal Reporting**: Generates a detailed multi-section report including:
    - **Overall Transaction Summary**: Total trades, cash inflow/outflow, net cash flow, overall realized P&L, and win rates computed across three dimensions (by Symbol, by Transaction/Round-Trip, and by Sell order).
    - **Account Summary**: Breakdown of trades, buy/sell values, and net cash flow for each individual account found in the transaction history.
    - **Detailed Round-Trip Ledger**: Individual trade entries formatted clearly with open/close dates, quantity, average buy/sell prices, cost basis, proceeds, and realized P&L.
    - **Multiple Report Sort Orders**: View the detailed round-trip ledger sorted alphabetically by symbol, descending by realized P&L (best to worst performers), or chronologically by date.

---

## Project Structure

- **[com/pluresideas/fidelitytradingtracker/parser](file:///Users/peter/Documents/Dev/FidelityTradingTracker/src/main/java/com/pluresideas/fidelitytradingtracker/parser)**: Handles raw text and CSV parser state machines.
- **[com/pluresideas/fidelitytradingtracker/model](file:///Users/peter/Documents/Dev/FidelityTradingTracker/src/main/java/com/pluresideas/fidelitytradingtracker/model)**: Data models representing transactions, account records, and round-trip trades.
- **[com/pluresideas/fidelitytradingtracker/service](file:///Users/peter/Documents/Dev/FidelityTradingTracker/src/main/java/com/pluresideas/fidelitytradingtracker/service)**: Core service logic for computing average cost basis and portfolio performance metrics.
- **[com/pluresideas/fidelitytradingtracker/report](file:///Users/peter/Documents/Dev/FidelityTradingTracker/src/main/java/com/pluresideas/fidelitytradingtracker/report)**: Text reporting engine to format results into comprehensive, sorted summaries.
- **[input_files](file:///Users/peter/Documents/Dev/FidelityTradingTracker/input_files)**: Standard directory for local configuration and data, including:
    - **Transaction Export**: A CSV log exported directly from a Fidelity account history page.
    - **Ignore List**: A plain text configuration containing symbols to omit from reporting metrics.

---

## Configuration

The application accepts the following command-line arguments:

1. **First argument (Mandatory)**: Path to the exported Fidelity CSV transaction history file.
2. **Second argument (Optional)**: Path to a plain text file containing stock symbols to ignore (one ticker per line, e.g. `SPAXX`). If omitted, all transactions are calculated without exclusions.

If the mandatory CSV file path is missing, the application will display a usage help menu and exit.

---

## How to Build and Run

### Option 1: Run with Maven

To compile and run the application using Maven, execute the following command from the root of the project directory.

With ignore symbols config file:

```bash
mvn compile exec:java -Dexec.mainClass="com.pluresideas.fidelitytradingtracker.Main" -Dexec.args="input_files/Accounts_History.csv input_files/IgnoreSymbols.txt"
```

Without ignore symbols config file:

```bash
mvn compile exec:java -Dexec.mainClass="com.pluresideas.fidelitytradingtracker.Main" -Dexec.args="input_files/Accounts_History.csv"
```

### Option 2: Run with Java

To run the application directly using the standard `java` command, compile the project and copy logging dependencies:

```bash
mvn compile dependency:copy-dependencies
```

#### **On macOS / Linux** (uses `:` as path separator):

```bash
# With ignore symbols config file
java -cp "target/classes:target/dependency/*" com.pluresideas.fidelitytradingtracker.Main input_files/Accounts_History.csv input_files/IgnoreSymbols.txt

# Without ignore symbols config file
java -cp "target/classes:target/dependency/*" com.pluresideas.fidelitytradingtracker.Main input_files/Accounts_History.csv
```

#### **On Windows** (uses `;` as path separator):

```bash
# With ignore symbols config file
java -cp "target/classes;target/dependency/*" com.pluresideas.fidelitytradingtracker.Main input_files/Accounts_History.csv input_files/IgnoreSymbols.txt

# Without ignore symbols config file
java -cp "target/classes;target/dependency/*" com.pluresideas.fidelitytradingtracker.Main input_files/Accounts_History.csv
```

### Option 3: Build a Native Executable (GraalVM)

Compile the project into a standalone platform-native binary (e.g. `fidelity-tracker` on macOS/Linux or `fidelity-tracker.exe` on Windows). This executable starts instantly and runs without requiring any Java JRE or Maven installed on the target machine.

#### Prerequisites
1. Install a GraalVM JDK (e.g., `brew install --cask graalvm-jdk` on macOS or via [SDKMAN](https://sdkman.io/) using `sdk install java 22.0.2-graalce`).
2. Ensure you have local developer compiler tools (e.g., `xcode-select --install` on macOS).
3. Set your `JAVA_HOME` environment variable to the GraalVM JDK directory (e.g., `export JAVA_HOME="/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home"`).

#### Compilation
Build the native binary by running:
```bash
mvn -Pnative native:compile
```

#### Run the Executable
Once completed, run the binary generated in the `target/` directory:
```bash
# With ignore symbols
./target/fidelity-tracker input_files/Accounts_History.csv input_files/IgnoreSymbols.txt

# Without ignore symbols
./target/fidelity-tracker input_files/Accounts_History.csv
```
