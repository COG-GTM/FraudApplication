package com.example.fraudworkflow.rest;

import com.example.fraudworkflow.ProcessVariables;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin REST facade over the Camunda engine for the human approval gate: start a case,
 * list the tasks waiting on an analyst, and complete the approval task. Consumed by the
 * React approval-gate UI.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CaseController {

    private static final String PROCESS_KEY = "fraud-alert-review";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public CaseController(RuntimeService runtimeService, TaskService taskService, HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    /** Start a fraud-alert-review case from a set of alert variables. */
    @PostMapping("/cases")
    public Map<String, Object> startCase(@RequestBody Map<String, Object> alert) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, alert);
        return caseStatus(instance.getProcessInstanceId());
    }

    /** List the human tasks currently awaiting an analyst. */
    @GetMapping("/tasks")
    public List<Map<String, Object>> openTasks() {
        return taskService.createTaskQuery().taskDefinitionKey("Task_AnalystReview").active().list()
                .stream().map(this::taskView).toList();
    }

    /** Complete the analyst approval task (approve = confirm escalation, reject = override/close). */
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(@PathVariable String taskId,
                                                            @RequestBody ApprovalDecision decision) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        String processInstanceId = task.getProcessInstanceId();
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.APPROVED, decision.approved());
        variables.put(ProcessVariables.ANALYST_NOTES, decision.analystNotes());
        taskService.complete(taskId, variables);
        return ResponseEntity.ok(caseStatus(processInstanceId));
    }

    /** Current status of a case (running task or final outcome), read from engine history. */
    @GetMapping("/cases/{id}")
    public Map<String, Object> caseStatus(@PathVariable("id") String processInstanceId) {
        Map<String, Object> status = new HashMap<>();
        status.put("processInstanceId", processInstanceId);

        Map<String, Object> variables = new HashMap<>();
        for (HistoricVariableInstance v : historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).list()) {
            variables.put(v.getName(), v.getValue());
        }
        status.put("variables", variables);

        HistoricProcessInstance historic = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        boolean ended = historic != null && historic.getEndTime() != null;
        status.put("ended", ended);

        Task openTask = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult();
        status.put("currentTaskId", openTask == null ? null : openTask.getId());
        status.put("currentTaskName", openTask == null ? null : openTask.getName());
        return status;
    }

    private Map<String, Object> taskView(Task task) {
        Map<String, Object> view = new HashMap<>();
        view.put("taskId", task.getId());
        view.put("name", task.getName());
        view.put("processInstanceId", task.getProcessInstanceId());
        view.put("alertId", runtimeService.getVariable(task.getExecutionId(), ProcessVariables.ALERT_ID));
        view.put("disposition", runtimeService.getVariable(task.getExecutionId(), ProcessVariables.DISPOSITION));
        view.put("riskScore", runtimeService.getVariable(task.getExecutionId(), ProcessVariables.RISK_SCORE));
        view.put("triageRationale", runtimeService.getVariable(task.getExecutionId(), ProcessVariables.TRIAGE_RATIONALE));
        return view;
    }

    /** Body of an analyst decision on the approval gate. */
    public record ApprovalDecision(boolean approved, String analystNotes) {
    }
}
