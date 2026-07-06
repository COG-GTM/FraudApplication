# Target Stack Standards — Pega → Camunda Migration

These are the engineering standards the migrated workflow must conform to. Devin uses
them as the target contract when generating the Camunda + containerised-Java
implementation from the Pega export.

## Workflow engine

- **Camunda 7 (Community)**, embedded in a Spring Boot 3 application via
  `camunda-bpm-spring-boot-starter`.
- Process models are **BPMN 2.0**, authored/editable in Camunda Modeler, deployed
  automatically from `src/main/resources` on application start.
- One BPMN process per Pega case type. Pega shapes map to BPMN as follows:

  | Pega shape | BPMN element |
  |------------|--------------|
  | Start / End events | `startEvent` / `endEvent` |
  | Utility (Activity) | `serviceTask` with `camunda:delegateExpression` |
  | Decision (When rule) | `exclusiveGateway` + `conditionExpression` |
  | Assignment (Worklist/Workbasket) | `userTask` with `candidateGroups` |
  | Ticket | boundary/error event (out of scope for this slice) |

## Java service layer (containerised)

- **Java 17**, Spring Boot 3.2.x, Maven.
- Each Pega Utility activity becomes a Spring `@Component` implementing
  `org.camunda.bpm.engine.delegate.JavaDelegate`, referenced from the BPMN by
  `camunda:delegateExpression="${beanName}"`. No business logic in the BPMN.
- Process variables are the integration contract. Delegates read/write typed
  variables (`alertId`, `disposition`, `riskScore`, `approved`, `analystNotes`).
- REST API exposed for orchestration: start a case, list human tasks, complete the
  approval task. JSON in/out, no server-side rendering.
- **Containerised**: a multi-stage `Dockerfile` producing a slim runtime image, plus a
  `docker-compose.yml` for one-command local startup. Stateless app; process state in
  the engine datastore (H2 for the demo, swappable for Postgres in production).

## Front end

- **React** single-page app for the human approval gate (Cockpit-style task list →
  approve/reject). Talks to the REST API only.
- For the demo it is delivered as a dependency-free single file (React via CDN) so it
  runs with no build step; in production this becomes a standard Vite/React app.

## Testing

- **Process tests** with `@SpringBootTest` driving the real engine services asserting
  every path through the model: auto-close, escalate-after-approval, and override-and-close.
- Delegates covered by unit tests. Target: green `mvn test` with no manual setup.

## Non-functional

- Full **audit trail**: Camunda history retains every task, variable and transition;
  delegates log decisions with rationale.
- Human approval gate is mandatory for any non-auto-close disposition — nothing is
  escalated or closed on a genuine exception without an analyst acting.
