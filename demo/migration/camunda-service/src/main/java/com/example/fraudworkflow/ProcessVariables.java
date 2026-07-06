package com.example.fraudworkflow;

/**
 * Central definition of the process-variable contract shared between the BPMN model,
 * the delegates and the REST layer.
 */
public final class ProcessVariables {

    private ProcessVariables() {
    }

    // Inputs
    public static final String ALERT_ID = "alertId";
    public static final String ALERT_TYPE = "alertType";
    public static final String CUSTOMER_REF = "customerRef";
    public static final String RISK_SCORE = "riskScore";
    public static final String COUNTERPARTY_TYPE = "counterpartyType";
    public static final String ACCOUNT_TYPE = "accountType";
    public static final String KYC_VERIFIED = "kycVerified";
    public static final String WHITELISTED_COUNTERPARTY = "whitelistedCounterparty";
    public static final String SANCTIONS_HIT = "sanctionsHit";
    public static final String PEP_MATCH = "pepMatch";
    public static final String STRUCTURING_PATTERN = "structuringPattern";
    public static final String PRIOR_SAR_COUNT = "priorSarCount";

    // Derived / decision
    public static final String DISPOSITION = "disposition";
    public static final String TRIAGE_RATIONALE = "triageRationale";
    public static final String ENRICHED = "enriched";

    // Human approval gate
    public static final String APPROVED = "approved";
    public static final String ANALYST_NOTES = "analystNotes";

    // Outcome
    public static final String CASE_OUTCOME = "caseOutcome";
    public static final String SAR_REFERENCE = "sarReference";

    // Disposition values
    public static final String DISP_AUTO_CLOSE = "AUTO_CLOSE_FALSE_POSITIVE";
    public static final String DISP_INVESTIGATE = "INVESTIGATE";
    public static final String DISP_ESCALATE = "ESCALATE_SAR";
}
