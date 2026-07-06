package com.example.fraudworkflow.delegate;

import com.example.fraudworkflow.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Migrated from the Pega "AutoCloseCase" utility (activity AutoCloseFraudCase). */
@Component("autoCloseCaseDelegate")
public class AutoCloseCaseDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AutoCloseCaseDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable(ProcessVariables.CASE_OUTCOME, "CLOSED_FALSE_POSITIVE");
        log.info("Auto-closed alert {} as a false positive", execution.getVariable(ProcessVariables.ALERT_ID));
    }
}
