package com.example.fraudapplication.triage.service;

import com.example.fraudapplication.triage.model.TriageAlert;
import com.example.fraudapplication.triage.model.TriageReport;
import com.example.fraudapplication.triage.model.TriageResult;

import java.util.List;

public interface TriageService {

    /** Triage a single alert. */
    TriageResult triage(TriageAlert alert);

    /** Triage a batch of alerts and produce an aggregate report. */
    TriageReport triageAll(List<TriageAlert> alerts);

    /** Load the configured sample dataset and triage every alert in it. */
    TriageReport triageSampleDataset();
}
