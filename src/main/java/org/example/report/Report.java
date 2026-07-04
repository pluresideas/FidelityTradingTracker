package org.example.report;

import org.example.service.CalculationResults;

public interface Report {
    void render(CalculationResults results);
}
