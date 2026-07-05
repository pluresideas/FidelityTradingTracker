package com.pluresideas.fidelitytradingtracker.report;

import com.pluresideas.fidelitytradingtracker.model.RoundTrip;
import com.pluresideas.fidelitytradingtracker.service.CalculationResults;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class HtmlReportGenerator {

    public void generate(CalculationResults r, String outputPath) throws IOException {
        // Sort closed roundtrips chronologically by close date
        List<RoundTrip> closedTrips = new ArrayList<>();
        for (RoundTrip rt : r.roundTrips()) {
            if (rt.isClosed()) {
                closedTrips.add(rt);
            }
        }

        closedTrips.sort((t1, t2) -> {
            String d1 = getComparableDate(t1.getCloseDate());
            String d2 = getComparableDate(t2.getCloseDate());
            int comp = d1.compareTo(d2);
            if (comp != 0) {
                return comp;
            }
            return t1.getSymbol().compareTo(t2.getSymbol());
        });

        // Compute running stats for the line chart
        StringBuilder chartDataJson = new StringBuilder("[");
        BigDecimal runningPnL = BigDecimal.ZERO;
        BigDecimal runningCost = BigDecimal.ZERO;

        for (int i = 0; i < closedTrips.size(); i++) {
            RoundTrip rt = closedTrips.get(i);
            runningPnL = runningPnL.add(rt.getRealizedPnL());
            runningCost = runningCost.add(rt.getTotalBuyValue());

            BigDecimal cumulativePct = BigDecimal.ZERO;
            if (runningCost.compareTo(BigDecimal.ZERO) > 0) {
                cumulativePct = runningPnL.multiply(new BigDecimal("100"))
                        .divide(runningCost, MathContext.DECIMAL128);
            }

            chartDataJson.append(String.format(
                "{\"date\":\"%s\",\"symbol\":\"%s\",\"pnl\":%s,\"cumPnL\":%s,\"cumPct\":%s}",
                rt.getCloseDate(),
                rt.getSymbol(),
                rt.getRealizedPnL().setScale(2, RoundingMode.HALF_UP).toString(),
                runningPnL.setScale(2, RoundingMode.HALF_UP).toString(),
                cumulativePct.setScale(2, RoundingMode.HALF_UP).toString()
            ));

            if (i < closedTrips.size() - 1) {
                chartDataJson.append(",");
            }
        }
        chartDataJson.append("]");

        // Build HTML template
        String html = getHtmlTemplate(r, chartDataJson.toString(), closedTrips);

        // Write HTML file to disk
        Files.writeString(Paths.get(outputPath), html);
    }

    private String getComparableDate(String dateStr) {
        if (dateStr == null) {
            return "";
        }
        String[] parts = dateStr.split("-");
        if (parts.length == 3) {
            return parts[2] + parts[0] + parts[1]; // YYYYMMDD
        }
        return dateStr;
    }

    private String getHtmlTemplate(CalculationResults r, String chartDataJson, List<RoundTrip> closedTrips) {
        // overall stats values
        String totalPnL = formatMoney(r.totalRealizedPnL());
        boolean isPnLNegative = r.totalRealizedPnL().compareTo(BigDecimal.ZERO) < 0;
        String pnlClass = isPnLNegative ? "danger" : "success";
        String pnlTextClass = isPnLNegative ? "text-danger" : "text-success";
        String winRate = String.format("%.1f%%", getWinRate(closedTrips));
        String reportPeriod = r.transactions().isEmpty() ? "N/A" : r.transactions().getFirst().date() + " to " + r.transactions().getLast().date();

        // Calculate detailed gain/loss stats
        int winningRoundTripsCount = 0;
        BigDecimal sumGains = BigDecimal.ZERO;
        int gainCount = 0;
        BigDecimal sumLosses = BigDecimal.ZERO;
        int lossCount = 0;

        for (RoundTrip rt : closedTrips) {
            BigDecimal pnl = rt.getRealizedPnL();
            BigDecimal pct = rt.getReturnPercentage();
            if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                winningRoundTripsCount++;
                sumGains = sumGains.add(pct);
                gainCount++;
            } else if (pnl.compareTo(BigDecimal.ZERO) < 0) {
                sumLosses = sumLosses.add(pct);
                lossCount++;
            }
        }

        BigDecimal avgGain = gainCount > 0 ? sumGains.divide(new BigDecimal(gainCount), MathContext.DECIMAL128) : BigDecimal.ZERO;
        BigDecimal avgLoss = lossCount > 0 ? sumLosses.divide(new BigDecimal(lossCount), MathContext.DECIMAL128) : BigDecimal.ZERO;

        String avgGainStr = gainCount > 0 ? String.format("+%,.2f%%", avgGain.setScale(2, RoundingMode.HALF_UP).doubleValue()) : "N/A";
        String avgLossStr = lossCount > 0 ? String.format("%,.2f%%", avgLoss.setScale(2, RoundingMode.HALF_UP).doubleValue()) : "N/A";
        double sellWinRate = r.totalSellsCount() > 0 ? ((double) r.winningSellsCount() / r.totalSellsCount() * 100.0) : 0.0;

        // Format transactions table rows
        StringBuilder tableRows = new StringBuilder();
        for (RoundTrip rt : r.roundTrips()) {
            boolean isLoss = rt.getRealizedPnL().compareTo(BigDecimal.ZERO) < 0;
            String rowPnlClass = isLoss ? "text-danger" : "text-success";
            String rowPctClass = isLoss ? "text-danger" : "text-success";
            String pnlSign = isLoss ? "" : "+";
            String pctSign = isLoss ? "" : "+";

            String pctVal = "-";
            if (rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pct = rt.getReturnPercentage().setScale(2, RoundingMode.HALF_UP);
                pctVal = pctSign + pct.toString() + "%";
            }

            tableRows.append(String.format(
                "<tr>" +
                "<td>%s</td>" +
                "<td>%s</td>" +
                "<td class=\"fw-bold text-primary\">%s</td>" +
                "<td>%s</td>" +
                "<td>%s</td>" +
                "<td>%s</td>" +
                "<td>%s</td>" +
                "<td>%s</td>" +
                "<td class=\"%s fw-bold\">%s%s</td>" +
                "<td class=\"%s fw-bold\">%s</td>" +
                "</tr>",
                rt.getOpenDate() != null ? rt.getOpenDate() : "N/A",
                rt.getCloseDate(),
                rt.getSymbol(),
                formatQty(rt.getTotalBuyQty()),
                formatMoney(rt.getAvgBuyPrice()),
                rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getAvgSellPrice()) : "-",
                formatMoney(rt.getTotalBuyValue()),
                rt.getTotalSellQty().compareTo(BigDecimal.ZERO) > 0 ? formatMoney(rt.getTotalSellValue()) : "-",
                rowPnlClass, pnlSign, formatMoney(rt.getRealizedPnL()),
                rowPctClass, pctVal
            ));
        }

        String template = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fidelity Portfolio Tracker</title>
    <!-- Google Fonts -->
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <!-- Bootstrap CSS (via CDN) -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- FontAwesome Icons -->
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <!-- ApexCharts (via CDN) -->
    <script src="https://cdn.jsdelivr.net/npm/apexcharts"></script>

    <style>
        :root {
            --bg-color: #0b0f19;
            --card-bg: rgba(22, 28, 45, 0.6);
            --border-color: rgba(255, 255, 255, 0.08);
            --accent-primary: #3b82f6;
            --accent-glow: rgba(59, 130, 246, 0.15);
            --success-color: #10b981;
            --danger-color: #ef4444;
        }

        body {
            background-color: var(--bg-color);
            color: #e2e8f0;
            font-family: 'Plus Jakarta Sans', sans-serif;
            min-height: 100vh;
            padding-bottom: 3rem;
            background-image: 
                radial-gradient(at 0% 0%, rgba(59, 130, 246, 0.1) 0px, transparent 50%),
                radial-gradient(at 50% 0%, rgba(16, 185, 129, 0.05) 0px, transparent 50%),
                radial-gradient(at 100% 100%, rgba(99, 102, 241, 0.08) 0px, transparent 50%);
            background-attachment: fixed;
        }

        h1, h2, h3, h4 {
            font-family: 'Outfit', sans-serif;
            font-weight: 600;
        }

        /* Glassmorphism Cards */
        .glass-card {
            background: var(--card-bg);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        .glass-card:hover {
            box-shadow: 0 12px 40px 0 rgba(59, 130, 246, 0.1);
        }

        .stat-card {
            padding: 1.5rem;
            position: relative;
            overflow: hidden;
        }

        .stat-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            width: 4px;
            height: 100%;
            background: var(--accent-primary);
        }

        .stat-card.success::before {
            background: var(--success-color);
        }

        .stat-card.danger::before {
            background: var(--danger-color);
        }

        .stat-label {
            font-size: 0.85rem;
            color: #94a3b8;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: 0.5rem;
        }

        .stat-value {
            font-size: 1.75rem;
            font-family: 'Outfit', sans-serif;
            font-weight: 700;
        }

        .text-success {
            color: var(--success-color) !important;
        }

        .text-danger {
            color: var(--danger-color) !important;
        }

        /* Tables */
        .custom-table-container {
            overflow-x: auto;
            border-radius: 12px;
        }

        .table {
            color: #cbd5e1;
            margin-bottom: 0;
        }

        .table th {
            background-color: rgba(15, 23, 42, 0.6);
            color: #94a3b8;
            font-weight: 600;
            text-transform: uppercase;
            font-size: 0.75rem;
            letter-spacing: 0.05em;
            padding: 1rem;
            border-bottom: 1px solid var(--border-color);
        }

        .table td {
            padding: 1rem;
            border-bottom: 1px solid rgba(255, 255, 255, 0.03);
            vertical-align: middle;
            font-size: 0.9rem;
        }

        .table tr:hover {
            background-color: rgba(255, 255, 255, 0.02);
        }

        /* Search input styling */
        .search-control {
            background: rgba(15, 23, 42, 0.4);
            border: 1px solid var(--border-color);
            color: #f8fafc;
            border-radius: 8px;
            padding: 0.6rem 1rem;
        }

        .search-control:focus {
            background: rgba(15, 23, 42, 0.6);
            border-color: var(--accent-primary);
            box-shadow: 0 0 0 2px var(--accent-glow);
            color: #f8fafc;
        }

        /* Scrollbars */
        ::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }

        ::-webkit-scrollbar-track {
            background: var(--bg-color);
        }

        ::-webkit-scrollbar-thumb {
            background: rgba(255, 255, 255, 0.1);
            border-radius: 4px;
        }

        ::-webkit-scrollbar-thumb:hover {
            background: rgba(255, 255, 255, 0.2);
        }
    </style>
</head>
<body>

    <div class="container py-4">
        <!-- Header -->
        <div class="d-flex justify-content-between align-items-center mb-4 pb-3 border-bottom border-secondary border-opacity-10">
            <div>
                <h1 class="text-white mb-1"><i class="fa-solid fa-chart-line text-primary me-2"></i>Fidelity Trading Tracker</h1>
                <p class="text-muted mb-0">Interactive Portfolio Performance Dashboard</p>
            </div>
            <span class="badge bg-secondary bg-opacity-20 text-secondary border border-secondary border-opacity-20 px-3 py-2 rounded-pill">
                <i class="fa-solid fa-clock me-1"></i>Report Period: {{period}}
            </span>
        </div>

        <!-- Stat Cards -->
        <div class="row g-3 mb-4">
            <div class="col-md-3">
                <div class="glass-card stat-card {{pnl_class}}">
                    <div class="stat-label">Net Realized P&L</div>
                    <div class="stat-value {{pnl_text_class}}">{{total_pnl}}</div>
                    <div class="stat-subtext mt-1 text-muted" style="font-size: 0.8rem; font-weight: 500;">
                        Avg Gain: <span class="text-success fw-bold">{{avg_gain}}</span> | Avg Loss: <span class="text-danger fw-bold">{{avg_loss}}</span>
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="glass-card stat-card">
                    <div class="stat-label">Win Rate</div>
                    <div class="stat-value text-white">{{win_rate}}</div>
                    <div class="stat-subtext mt-1 text-muted" style="font-size: 0.8rem; font-weight: 500;">
                        <span class="text-success fw-bold">{{winning_trades}}</span> wins out of <span class="fw-bold">{{closed_trades_count}}</span> completed trades
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="glass-card stat-card">
                    <div class="stat-label">Total Completed Trades</div>
                    <div class="stat-value text-white">{{closed_trades_count}}</div>
                    <div class="stat-subtext mt-1 text-muted" style="font-size: 0.8rem; font-weight: 500;">
                        From <span class="fw-bold">{{total_trades}}</span> total order executions in file
                    </div>
                </div>
            </div>
            <div class="col-md-3">
                <div class="glass-card stat-card">
                    <div class="stat-label">Sell Win Rate</div>
                    <div class="stat-value text-white">{{sell_win_rate}}</div>
                    <div class="stat-subtext mt-1 text-muted" style="font-size: 0.8rem; font-weight: 500;">
                        <span class="text-success fw-bold">{{winning_sells}}</span> profitable sells out of <span class="fw-bold">{{total_sells}}</span> sells
                    </div>
                </div>
            </div>
        </div>

        <!-- Graph Section -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="glass-card p-4">
                    <h5 class="text-white mb-3"><i class="fa-solid fa-chart-area text-primary me-2"></i>Cumulative P&L % Over Time</h5>
                    <div id="chart"></div>
                </div>
            </div>
        </div>

        <!-- Trades Ledger -->
        <div class="row">
            <div class="col-12">
                <div class="glass-card p-4">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="text-white mb-0"><i class="fa-solid fa-list-check text-primary me-2"></i>Round-Trip Ledger</h5>
                        <input type="text" id="tableSearch" class="form-control w-25 search-control" placeholder="Search by Symbol...">
                    </div>
                    <div class="custom-table-container">
                        <table class="table table-hover align-middle" id="tradesTable">
                            <thead>
                                <tr>
                                    <th>Open Date</th>
                                    <th>Close Date</th>
                                    <th>Symbol</th>
                                    <th>Quantity</th>
                                    <th>Avg Buy</th>
                                    <th>Avg Sell</th>
                                    <th>Cost Basis</th>
                                    <th>Proceeds</th>
                                    <th>Realized P&L</th>
                                    <th>% P&L</th>
                                </tr>
                            </thead>
                            <tbody>
                                {{table_rows}}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Chart Configuration Script -->
    <script>
        // Injected chart data
        const chartData = {{chart_data}};

        // Extract dates and values
        const dates = chartData.map(d => d.date);
        const percentages = chartData.map(d => parseFloat(d.cumPct));
        const pnlDollars = chartData.map(d => parseFloat(d.cumPnL));

        const options = {
            series: [{
                name: 'Cumulative Return (%)',
                data: percentages
            }],
            chart: {
                type: 'area',
                height: 380,
                background: 'transparent',
                foreColor: '#94a3b8',
                toolbar: {
                    show: true,
                    tools: {
                        download: true,
                        selection: false,
                        zoom: true,
                        zoomin: true,
                        zoomout: true,
                        pan: true,
                        reset: true
                    }
                },
                zoom: {
                    enabled: true
                }
            },
            colors: ['#3b82f6'],
            stroke: {
                curve: 'smooth',
                width: 3
            },
            fill: {
                type: 'gradient',
                gradient: {
                    shadeIntensity: 1,
                    opacityFrom: 0.45,
                    opacityTo: 0.05,
                    stops: [0, 90, 100]
                }
            },
            grid: {
                borderColor: 'rgba(255, 255, 255, 0.05)',
                strokeDashArray: 4
            },
            xaxis: {
                categories: dates,
                axisBorder: {
                    show: false
                },
                axisTicks: {
                    show: false
                }
            },
            yaxis: {
                labels: {
                    formatter: function (value) {
                        return (value >= 0 ? '+' : '') + value.toFixed(2) + '%';
                    }
                }
            },
            tooltip: {
                theme: 'dark',
                x: {
                    show: true
                },
                y: {
                    formatter: function (val, { dataPointIndex }) {
                        const dollarVal = pnlDollars[dataPointIndex];
                        const symbol = chartData[dataPointIndex].symbol;
                        const pnl = chartData[dataPointIndex].pnl;
                        const sign = dollarVal >= 0 ? '$' : '-$';
                        const pnlSign = pnl >= 0 ? '+' : '';
                        
                        return '<span class="fw-bold text-info">' + (val >= 0 ? '+' : '') + val.toFixed(2) + '%</span><br/>' +
                               '<span class="text-muted">Cum P&L:</span> <span class="fw-bold">' + (dollarVal >= 0 ? '+' : '-') + '$' + Math.abs(dollarVal).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2}) + '</span><br/>' +
                               '<span class="text-muted">Closed:</span> <span class="fw-bold">' + symbol + ' (' + (pnl >= 0 ? '+' : '-') + '$' + Math.abs(pnl).toFixed(2) + ')</span>';
                    }
                }
            }
        };

        const chart = new ApexCharts(document.querySelector("#chart"), options);
        chart.render();

        // Search feature
        document.getElementById('tableSearch').addEventListener('keyup', function() {
            const query = this.value.toUpperCase();
            const rows = document.querySelectorAll('#tradesTable tbody tr');
            
            rows.forEach(row => {
                const symbolCell = row.cells[2].textContent.toUpperCase();
                if (symbolCell.indexOf(query) > -1) {
                    row.style.display = "";
                } else {
                    row.style.display = "none";
                }
            });
        });
    </script>
</body>
</html>
""";

        return template
            .replace("{{period}}", reportPeriod)
            .replace("{{pnl_class}}", pnlClass)
            .replace("{{pnl_text_class}}", pnlTextClass)
            .replace("{{total_pnl}}", totalPnL)
            .replace("{{avg_gain}}", avgGainStr)
            .replace("{{avg_loss}}", avgLossStr)
            .replace("{{win_rate}}", winRate)
            .replace("{{winning_trades}}", String.valueOf(winningRoundTripsCount))
            .replace("{{closed_trades_count}}", String.valueOf(closedTrips.size()))
            .replace("{{total_trades}}", String.valueOf(r.transactions().size()))
            .replace("{{sell_win_rate}}", String.valueOf(String.format("%.1f%%", sellWinRate)))
            .replace("{{winning_sells}}", String.valueOf(r.winningSellsCount()))
            .replace("{{total_sells}}", String.valueOf(r.totalSellsCount()))
            .replace("{{table_rows}}", tableRows.toString())
            .replace("{{chart_data}}", chartDataJson);
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) {
            return "-";
        }
        BigDecimal rounded = val.setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.ZERO) < 0) {
            return String.format("-$%,.2f", rounded.abs().doubleValue());
        } else {
            return String.format("$%,.2f", rounded.doubleValue());
        }
    }

    private String formatQty(BigDecimal val) {
        if (val == null) {
            return "-";
        }
        DecimalFormat df = new DecimalFormat("#,##0.######");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(val);
    }

    private double getWinRate(List<RoundTrip> closedTrips) {
        if (closedTrips.isEmpty()) {
            return 0.0;
        }
        long wins = closedTrips.stream()
                .filter(rt -> rt.getRealizedPnL().compareTo(BigDecimal.ZERO) > 0)
                .count();
        return ((double) wins / closedTrips.size()) * 100.0;
    }
}
