package com.example.fraudapplication.triage;

import com.example.fraudapplication.triage.model.AlertContext;
import com.example.fraudapplication.triage.model.TriageAlert;
import com.example.fraudapplication.triage.model.TriageDisposition;
import com.example.fraudapplication.triage.model.TriageResult;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Deterministic, auditable implementation of the triage decision rules documented in
 * {@code demo/triage/triage-rules.md}. Rules are evaluated top to bottom and the first
 * one that matches sets the disposition. Every result carries the rule number that
 * fired and a plain-English rationale.
 */
@Component
public class TriageRuleEngine {

    private static final int HIGH_RISK_SCORE = 80;
    private static final int AUTO_CLOSE_RISK_CEILING = 60;

    private static final Set<String> AUTO_CLOSE_HIGH_TXN_COUNTERPARTIES =
            Set.of("RECURRING_PAYEE", "MERCHANT");

    public TriageResult evaluate(TriageAlert alert) {
        AlertContext ctx = alert.getContext();
        String type = alert.getAlertType();

        // Rule 1: financial-crime red flags.
        String redFlag = firstRedFlag(ctx);
        if (redFlag != null) {
            return result(alert, TriageDisposition.ESCALATE_SAR, 1,
                    "Financial-crime red flag present (" + redFlag
                            + "); routed to the SAR / EDD queue for a financial-crime investigator.");
        }

        // Rule 2: high model risk.
        if (alert.getRiskScore() >= HIGH_RISK_SCORE) {
            return result(alert, TriageDisposition.ESCALATE_SAR, 2,
                    "Model risk score " + alert.getRiskScore() + " is at or above the escalation "
                            + "threshold of " + HIGH_RISK_SCORE + "; escalated for enhanced review.");
        }

        // Rule 3: known-good patterns (only below the auto-close risk ceiling).
        if (alert.getRiskScore() < AUTO_CLOSE_RISK_CEILING) {
            String autoCloseReason = autoCloseReason(type, ctx);
            if (autoCloseReason != null) {
                return result(alert, TriageDisposition.AUTO_CLOSE_FALSE_POSITIVE, 3,
                        autoCloseReason + " Risk score " + alert.getRiskScore()
                                + " is below the auto-close ceiling of " + AUTO_CLOSE_RISK_CEILING
                                + "; closed as a false positive with rationale logged for audit.");
            }
        }

        // Rule 4: default.
        return result(alert, TriageDisposition.INVESTIGATE, 4,
                "No financial-crime red flags and does not match a known-good pattern; "
                        + "routed to a fraud analyst for review (risk score " + alert.getRiskScore() + ").");
    }

    private String firstRedFlag(AlertContext ctx) {
        if (ctx.isSanctionsHit()) {
            return "sanctions match";
        }
        if (ctx.isPepMatch()) {
            return "politically exposed person match";
        }
        if (ctx.isStructuringPattern()) {
            return "structuring pattern";
        }
        if (ctx.getPriorSarCount() >= 1) {
            return "prior SAR on file (" + ctx.getPriorSarCount() + ")";
        }
        return null;
    }

    private String autoCloseReason(String type, AlertContext ctx) {
        switch (type) {
            case "HIGH_TRANSACTION":
                if (ctx.isKycVerified()
                        && (AUTO_CLOSE_HIGH_TXN_COUNTERPARTIES.contains(ctx.getCounterpartyType())
                        || ctx.isWhitelistedCounterparty())) {
                    return "High-value payment to a "
                            + (ctx.isWhitelistedCounterparty() ? "whitelisted" : "recurring/known")
                            + " counterparty with verified KYC.";
                }
                return null;
            case "PING_PONG":
                if ("INTERNAL_ACCOUNT".equals(ctx.getCounterpartyType())) {
                    return "Ping-pong movement is between the customer's own internal accounts.";
                }
                return null;
            case "MULTIPLE_SERVICE":
                if ("BUSINESS".equals(ctx.getAccountType())) {
                    return "Multiple-service activity on a registered business account is expected.";
                }
                return null;
            default:
                return null;
        }
    }

    private TriageResult result(TriageAlert alert, TriageDisposition disposition, int rule, String rationale) {
        return TriageResult.builder()
                .alertId(alert.getAlertId())
                .alertType(alert.getAlertType())
                .customerRef(alert.getCustomerRef())
                .riskScore(alert.getRiskScore())
                .disposition(disposition)
                .ruleFired(rule)
                .rationale(rationale)
                .build();
    }
}
