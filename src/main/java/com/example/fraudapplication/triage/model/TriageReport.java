package com.example.fraudapplication.triage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Aggregate view over a batch of triaged alerts. This is the review-ready summary
 * the room sees: how many alerts auto-closed vs. needed a human, plus every
 * individual disposition with its rationale.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageReport {

    private int total;
    private Map<TriageDisposition, Long> countsByDisposition;
    private double autoCloseRate;
    private List<TriageResult> results;
}
