package org.pluresideas.fidelitytradingtracker.report;

import org.pluresideas.fidelitytradingtracker.service.CalculationResults;

public interface Report {
    void render(CalculationResults results);
}
