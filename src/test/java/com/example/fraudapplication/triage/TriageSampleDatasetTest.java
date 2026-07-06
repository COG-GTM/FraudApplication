package com.example.fraudapplication.triage;

import com.example.fraudapplication.triage.model.TriageAlert;
import com.example.fraudapplication.triage.model.TriageDisposition;
import com.example.fraudapplication.triage.model.TriageReport;
import com.example.fraudapplication.triage.service.TriageServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that the shipped sample dataset produces the documented
 * disposition distribution and the roughly two-thirds auto-close rate that backs the
 * demo talking point.
 */
class TriageSampleDatasetTest {

    private final AlertDatasetLoader loader = new AlertDatasetLoader();
    private final TriageServiceImpl service =
            new TriageServiceImpl(new TriageRuleEngine(), loader, "src/main/resources/triage/alerts.json");

    @Test
    void datasetLoadsAllAlerts() {
        List<TriageAlert> alerts = loader.load("src/main/resources/triage/alerts.json");
        assertEquals(24, alerts.size());
        alerts.forEach(a -> assertNotNull(a.getContext(), "context missing for " + a.getAlertId()));
    }

    @Test
    void sampleDatasetHasDocumentedDistribution() {
        TriageReport report = service.triageSampleDataset();

        assertEquals(24, report.getTotal());
        assertEquals(16L, report.getCountsByDisposition().get(TriageDisposition.AUTO_CLOSE_FALSE_POSITIVE));
        assertEquals(5L, report.getCountsByDisposition().get(TriageDisposition.INVESTIGATE));
        assertEquals(3L, report.getCountsByDisposition().get(TriageDisposition.ESCALATE_SAR));
    }

    @Test
    void autoCloseRateIsAroundTwoThirds() {
        TriageReport report = service.triageSampleDataset();
        assertTrue(report.getAutoCloseRate() > 0.6 && report.getAutoCloseRate() < 0.7,
                "expected auto-close rate ~0.67 but was " + report.getAutoCloseRate());
    }

    @Test
    void everyResultHasRationaleAndRule() {
        TriageReport report = service.triageSampleDataset();
        report.getResults().forEach(r -> {
            assertNotNull(r.getRationale());
            assertTrue(r.getRationale().length() > 10);
            assertTrue(r.getRuleFired() >= 1 && r.getRuleFired() <= 4);
        });
    }
}
