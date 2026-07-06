package com.example.fraudworkflow;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Executes every path through the migrated fraud-alert-review process: auto-close of a
 * false positive, escalation to SAR after analyst approval, and analyst override/close.
 */
@SpringBootTest
class FraudAlertReviewProcessTest {

    private static final String PROCESS_KEY = "fraud-alert-review";

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;

    private Map<String, Object> autoCloseAlert() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(ProcessVariables.ALERT_ID, "ALRT-AC");
        vars.put(ProcessVariables.ALERT_TYPE, "HIGH_TRANSACTION");
        vars.put(ProcessVariables.RISK_SCORE, 30);
        vars.put(ProcessVariables.COUNTERPARTY_TYPE, "RECURRING_PAYEE");
        vars.put(ProcessVariables.KYC_VERIFIED, true);
        return vars;
    }

    private Map<String, Object> investigateAlert() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(ProcessVariables.ALERT_ID, "ALRT-INV");
        vars.put(ProcessVariables.ALERT_TYPE, "HIGH_TRANSACTION");
        vars.put(ProcessVariables.RISK_SCORE, 68);
        vars.put(ProcessVariables.COUNTERPARTY_TYPE, "NEW_PAYEE");
        vars.put(ProcessVariables.KYC_VERIFIED, true);
        return vars;
    }

    private Object variable(String processInstanceId, String name) {
        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).variableName(name).singleResult().getValue();
    }

    private boolean ended(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult().getEndTime() != null;
    }

    @Test
    void falsePositiveAutoClosesWithoutHumanTask() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, autoCloseAlert());

        assertThat(ended(instance.getId())).isTrue();
        assertThat(taskService.createTaskQuery().processInstanceId(instance.getId()).count()).isZero();
        assertThat(variable(instance.getId(), ProcessVariables.DISPOSITION))
                .isEqualTo(ProcessVariables.DISP_AUTO_CLOSE);
        assertThat(variable(instance.getId(), ProcessVariables.CASE_OUTCOME))
                .isEqualTo("CLOSED_FALSE_POSITIVE");
    }

    @Test
    void genuineExceptionWaitsAtApprovalGateThenEscalatesOnApproval() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, investigateAlert());

        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("Task_AnalystReview");
        assertThat(ended(instance.getId())).isFalse();

        Map<String, Object> decision = new HashMap<>();
        decision.put(ProcessVariables.APPROVED, true);
        decision.put(ProcessVariables.ANALYST_NOTES, "Confirmed suspicious; filing SAR.");
        taskService.complete(task.getId(), decision);

        assertThat(ended(instance.getId())).isTrue();
        assertThat(variable(instance.getId(), ProcessVariables.CASE_OUTCOME)).isEqualTo("ESCALATED_TO_SAR");
        assertThat(variable(instance.getId(), ProcessVariables.SAR_REFERENCE)).isEqualTo("SAR-ALRT-INV");
    }

    @Test
    void analystCanOverrideAndCloseAtApprovalGate() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, investigateAlert());

        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
        Map<String, Object> decision = new HashMap<>();
        decision.put(ProcessVariables.APPROVED, false);
        decision.put(ProcessVariables.ANALYST_NOTES, "Benign after review.");
        taskService.complete(task.getId(), decision);

        assertThat(ended(instance.getId())).isTrue();
        assertThat(variable(instance.getId(), ProcessVariables.CASE_OUTCOME)).isEqualTo("CLOSED_BY_ANALYST");
    }

    @Test
    void presetDispositionIsRespected() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(ProcessVariables.ALERT_ID, "ALRT-PRESET");
        vars.put(ProcessVariables.DISPOSITION, ProcessVariables.DISP_AUTO_CLOSE);
        // No alert feature variables supplied; the preset must be honoured.
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, vars);

        assertThat(ended(instance.getId())).isTrue();
        assertThat(variable(instance.getId(), ProcessVariables.CASE_OUTCOME)).isEqualTo("CLOSED_FALSE_POSITIVE");
    }
}
