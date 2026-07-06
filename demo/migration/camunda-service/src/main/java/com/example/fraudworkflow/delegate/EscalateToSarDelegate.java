package com.example.fraudworkflow.delegate;

import com.example.fraudworkflow.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Migrated from the Pega "EscalateToSAR" utility (activity RaiseSARReferral). */
@Component("escalateToSarDelegate")
public class EscalateToSarDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(EscalateToSarDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        String sarReference = "SAR-" + execution.getVariable(ProcessVariables.ALERT_ID);
        execution.setVariable(ProcessVariables.CASE_OUTCOME, "ESCALATED_TO_SAR");
        execution.setVariable(ProcessVariables.SAR_REFERENCE, sarReference);
        log.info("Escalated alert {} to the SAR queue as {}",
                execution.getVariable(ProcessVariables.ALERT_ID), sarReference);
    }
}
