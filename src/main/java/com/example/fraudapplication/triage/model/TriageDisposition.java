package com.example.fraudapplication.triage.model;

/**
 * The outcome the triage engine assigns to an alert.
 */
public enum TriageDisposition {

    /** Benign activity that matched a heuristic; closed automatically with rationale. */
    AUTO_CLOSE_FALSE_POSITIVE,

    /** No financial-crime red flags but not obviously benign; needs analyst review. */
    INVESTIGATE,

    /** Financial-crime red flag present; route to the SAR / enhanced due diligence queue. */
    ESCALATE_SAR
}
