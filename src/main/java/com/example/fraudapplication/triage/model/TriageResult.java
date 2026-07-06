package com.example.fraudapplication.triage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The disposition assigned to a single alert, with the rationale and the rule that
 * fired so the decision is fully auditable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageResult {

    private String alertId;
    private String alertType;
    private String customerRef;
    private int riskScore;
    private TriageDisposition disposition;
    private int ruleFired;
    private String rationale;

    /** True when a human still needs to act (anything other than an auto-close). */
    public boolean isRequiresHumanReview() {
        return disposition != TriageDisposition.AUTO_CLOSE_FALSE_POSITIVE;
    }
}
