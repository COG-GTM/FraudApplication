package com.example.fraudworkflow.delegate;

import com.example.fraudworkflow.ProcessVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Migrated from the Pega "CloseCase" utility (activity CloseFraudCase) — analyst override. */
@Component("closeCaseDelegate")
public class CloseCaseDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CloseCaseDelegate.class);

    @Override
    public void execute(DelegateExecution execution) {
        execution.setVariable(ProcessVariables.CASE_OUTCOME, "CLOSED_BY_ANALYST");
        log.info("Closed alert {} by analyst override", execution.getVariable(ProcessVariables.ALERT_ID));
    }
}
