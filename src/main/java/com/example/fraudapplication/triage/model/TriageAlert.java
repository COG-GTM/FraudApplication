package com.example.fraudapplication.triage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single fraud alert as delivered to the triage queue. Mirrors the structure of
 * {@code demo/triage/alerts/alerts.json}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriageAlert {

    private String alertId;
    private String alertType;
    private String customerRef;
    private double amount;
    private String currency;
    private String detectedAt;
    private int riskScore;
    private AlertContext context;
}
