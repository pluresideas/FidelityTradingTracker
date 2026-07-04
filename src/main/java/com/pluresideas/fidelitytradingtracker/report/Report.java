package com.pluresideas.fidelitytradingtracker.report;

import com.pluresideas.fidelitytradingtracker.service.CalculationResults;

public interface Report {
    void render(CalculationResults results);
}
