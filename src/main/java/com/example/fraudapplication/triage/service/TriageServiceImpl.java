package com.example.fraudapplication.triage.service;

import com.example.fraudapplication.triage.AlertDatasetLoader;
import com.example.fraudapplication.triage.TriageRuleEngine;
import com.example.fraudapplication.triage.model.TriageAlert;
import com.example.fraudapplication.triage.model.TriageDisposition;
import com.example.fraudapplication.triage.model.TriageReport;
import com.example.fraudapplication.triage.model.TriageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TriageServiceImpl implements TriageService {

    private final TriageRuleEngine ruleEngine;
    private final AlertDatasetLoader datasetLoader;
    private final String datasetPath;

    public TriageServiceImpl(TriageRuleEngine ruleEngine,
                             AlertDatasetLoader datasetLoader,
                             @Value("${triage.alerts.path:demo/triage/alerts/alerts.json}") String datasetPath) {
        this.ruleEngine = ruleEngine;
        this.datasetLoader = datasetLoader;
        this.datasetPath = datasetPath;
    }

    @Override
    public TriageResult triage(TriageAlert alert) {
        return ruleEngine.evaluate(alert);
    }

    @Override
    public TriageReport triageAll(List<TriageAlert> alerts) {
        List<TriageResult> results = alerts.stream().map(ruleEngine::evaluate).toList();

        Map<TriageDisposition, Long> counts = new EnumMap<>(TriageDisposition.class);
        for (TriageDisposition disposition : TriageDisposition.values()) {
            counts.put(disposition, 0L);
        }
        for (TriageResult result : results) {
            counts.merge(result.getDisposition(), 1L, Long::sum);
        }

        int total = results.size();
        long autoClosed = counts.get(TriageDisposition.AUTO_CLOSE_FALSE_POSITIVE);
        double autoCloseRate = total == 0 ? 0.0 : (double) autoClosed / total;

        return TriageReport.builder()
                .total(total)
                .countsByDisposition(counts)
                .autoCloseRate(autoCloseRate)
                .results(results)
                .build();
    }

    @Override
    public TriageReport triageSampleDataset() {
        List<TriageAlert> alerts = datasetLoader.load(datasetPath);
        return triageAll(alerts);
    }
}
