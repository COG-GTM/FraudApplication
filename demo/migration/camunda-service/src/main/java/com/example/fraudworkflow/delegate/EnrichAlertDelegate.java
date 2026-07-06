package com.example.fraudworkflow.delegate;

import com.example.fraudworkflow.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Migrated from the Pega "EnrichAlert" utility (activity EnrichFraudAlert). Gathers the
 * context needed for triage. In the demo this simply flags the alert as enriched; in
 * production it would call the customer/KYC/sanctions enrichment services.
 */
@Component("enrichAlertDelegate")
public class EnrichAlertDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EnrichAlertDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String alertId = (String) execution.getVariable(ProcessVariables.ALERT_ID);
        log.info("Enriching alert {}", alertId);
        execution.setVariable(ProcessVariables.ENRICHED, true);
    }
}
