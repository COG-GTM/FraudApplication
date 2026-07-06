package com.example.fraudapplication.triage.controller;

import com.example.fraudapplication.triage.model.TriageAlert;
import com.example.fraudapplication.triage.model.TriageReport;
import com.example.fraudapplication.triage.model.TriageResult;
import com.example.fraudapplication.triage.service.TriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the first-pass triage engine over HTTP so the disposition report can be
 * driven live in the demo and consumed by the migrated Camunda workflow.
 */
@RestController
@RequestMapping("fraud/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    /** Triage the bundled sample dataset and return the aggregate report. */
    @GetMapping
    public TriageReport triageSample() {
        return triageService.triageSampleDataset();
    }

    /** Triage a batch of alerts supplied in the request body. */
    @PostMapping
    public TriageReport triageBatch(@RequestBody List<TriageAlert> alerts) {
        return triageService.triageAll(alerts);
    }

    /** Triage a single alert. */
    @PostMapping("/one")
    public TriageResult triageOne(@RequestBody TriageAlert alert) {
        return triageService.triage(alert);
    }
}
