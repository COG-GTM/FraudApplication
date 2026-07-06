package com.example.fraudworkflow.delegate;

import com.example.fraudworkflow.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Migrated from the Pega "AutomatedTriage" utility (activity RunAutomatedTriage) and the
 * "IsAutoClose" decision. Applies the first-match-wins triage rules from
 * {@code demo/triage/triage-rules.md} and sets the {@code disposition} variable that the
 * downstream exclusive gateway routes on.
 *
 * <p>If a {@code disposition} has already been supplied on the process (e.g. produced by the
 * standalone triage engine) it is respected; otherwise it is computed from the alert
 * variables so the workflow is self-contained.</p>
 */
@Component("automatedTriageDelegate")
public class AutomatedTriageDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AutomatedTriageDelegate.class);

    private static final int HIGH_RISK_SCORE = 80;
    private static final int AUTO_CLOSE_RISK_CEILING = 60;
    private static final Set<String> AUTO_CLOSE_HIGH_TXN_COUNTERPARTIES =
            Set.of("RECURRING_PAYEE", "MERCHANT");

    @Override
    public void execute(DelegateExecution execution) {
        String preset = (String) execution.getVariable(ProcessVariables.DISPOSITION);
        if (preset != null && !preset.isBlank()) {
            log.info("Alert {} already carries disposition {}",
                    execution.getVariable(ProcessVariables.ALERT_ID), preset);
            return;
        }

        int riskScore = asInt(execution.getVariable(ProcessVariables.RISK_SCORE));
        boolean sanctionsHit = asBool(execution.getVariable(ProcessVariables.SANCTIONS_HIT));
        boolean pepMatch = asBool(execution.getVariable(ProcessVariables.PEP_MATCH));
        boolean structuring = asBool(execution.getVariable(ProcessVariables.STRUCTURING_PATTERN));
        int priorSar = asInt(execution.getVariable(ProcessVariables.PRIOR_SAR_COUNT));
        boolean kyc = asBool(execution.getVariable(ProcessVariables.KYC_VERIFIED));
        boolean whitelisted = asBool(execution.getVariable(ProcessVariables.WHITELISTED_COUNTERPARTY));
        String type = asString(execution.getVariable(ProcessVariables.ALERT_TYPE));
        String counterparty = asString(execution.getVariable(ProcessVariables.COUNTERPARTY_TYPE));
        String accountType = asString(execution.getVariable(ProcessVariables.ACCOUNT_TYPE));

        String disposition;
        String rationale;

        if (sanctionsHit || pepMatch || structuring || priorSar >= 1) {
            disposition = ProcessVariables.DISP_ESCALATE;
            rationale = "Financial-crime red flag present; routed to the SAR / EDD queue.";
        } else if (riskScore >= HIGH_RISK_SCORE) {
            disposition = ProcessVariables.DISP_ESCALATE;
            rationale = "Model risk score " + riskScore + " at or above escalation threshold.";
        } else if (riskScore < AUTO_CLOSE_RISK_CEILING && matchesAutoClose(type, counterparty, accountType, kyc, whitelisted)) {
            disposition = ProcessVariables.DISP_AUTO_CLOSE;
            rationale = "Matches a known-good pattern with low risk; auto-closed as a false positive.";
        } else {
            disposition = ProcessVariables.DISP_INVESTIGATE;
            rationale = "No red flags and not a known-good pattern; routed to a fraud analyst.";
        }

        execution.setVariable(ProcessVariables.DISPOSITION, disposition);
        execution.setVariable(ProcessVariables.TRIAGE_RATIONALE, rationale);
        log.info("Triaged alert {} -> {} ({})",
                execution.getVariable(ProcessVariables.ALERT_ID), disposition, rationale);
    }

    private boolean matchesAutoClose(String type, String counterparty, String accountType,
                                     boolean kyc, boolean whitelisted) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case "HIGH_TRANSACTION":
                return kyc && (AUTO_CLOSE_HIGH_TXN_COUNTERPARTIES.contains(counterparty) || whitelisted);
            case "PING_PONG":
                return "INTERNAL_ACCOUNT".equals(counterparty);
            case "MULTIPLE_SERVICE":
                return "BUSINESS".equals(accountType);
            default:
                return false;
        }
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean asBool(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
