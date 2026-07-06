# Fraud & Financial Crime — Devin Demo

**First-pass alert triage, plus a Pega → Camunda migration slice.**

This package demonstrates two things a fraud & financial-crime team cares about:

1. **Triage** — Devin cuts the false-positive load. A batch of alerts is triaged in
   parallel, each with a written rationale, and only genuine exceptions are escalated
   for human review.
2. **Migration** — Devin moves a workflow off Pega. A representative Pega case flow and
   its BPMN are migrated to a target stack (Camunda + containerised Java + React) with
   tests and a human approval gate — a review-ready PR.

The team's own framing: the problem is *operations complexity, not model accuracy*.

---

## Layout

```
demo/
├── triage/
│   ├── alerts/alerts.json          # 24 synthetic, anonymised alerts
│   ├── triage-rules.md             # the first-pass triage decision rules
│   └── sample-disposition-report.json   # generated audit trail (rationale per alert)
└── migration/
    ├── pega/FraudAlertReview.flow.xml   # representative Pega case-flow export (the "before")
    ├── bpmn/fraud-alert-review.bpmn      # BPMN 2.0 model derived from the Pega flow
    ├── target-stack-standards.md         # Camunda + Java + React standards (the target contract)
    ├── camunda-service/                  # migrated Spring Boot + Camunda 7 service (the "after")
    └── approval-ui/index.html            # React human-approval-gate UI
```

The triage engine itself lives in the main application under
`src/main/java/com/example/fraudapplication/triage/` and is exercised over HTTP at
`GET /fraud/triage`.

---

## Part 1 — Triage

**Inputs the room sees:** `triage/alerts/alerts.json` (24 alerts across the three fraud
patterns, with clear false positives and a handful of genuine exceptions) and the rules
in `triage/triage-rules.md`.

**What runs:** the triage engine applies the first-match-wins rules and assigns every
alert a disposition (`AUTO_CLOSE_FALSE_POSITIVE`, `INVESTIGATE`, `ESCALATE_SAR`) with a
plain-English rationale and the rule number that fired — a full audit trail.

Run it:

```bash
# from the repo root
./mvnw spring-boot:run
curl -s localhost:8080/fraud/triage | jq
```

On the sample dataset the result is **16 auto-closed / 5 investigate / 3 escalate**
(~67% auto-close) — the concrete backing for the "~70% of alerts are false positives"
talking point. This distribution is asserted by
`TriageSampleDatasetTest` so it stays honest.

> In the live demo this is where Devin runs **parallel sessions**, one per alert, each
> investigating and drafting a disposition. The engine here is the deterministic,
> review-ready version of that output.

---

## Part 2 — Pega → Camunda migration

**Inputs the room sees:** the Pega export (`migration/pega/FraudAlertReview.flow.xml`),
the BPMN it maps to (`migration/bpmn/fraud-alert-review.bpmn`, open it in Camunda
Modeler), and the target standards (`migration/target-stack-standards.md`).

**The migrated workflow** (`migration/camunda-service/`):

```
Alert received → Enrich → Automated triage → [auto-close?]
   ├── false positive → Auto-close case → (end)
   └── needs review    → Analyst review (HUMAN APPROVAL GATE) → [escalation confirmed?]
                            ├── confirmed → Escalate to SAR queue → (end)
                            └── override  → Close case → (end)
```

Each Pega utility became a Spring `JavaDelegate`; each Pega decision became an exclusive
gateway; the Pega assignment became a BPMN `userTask` — the mandatory human approval gate.

Run the tests (all three paths + preset disposition):

```bash
./mvnw -f demo/migration/camunda-service/pom.xml test
```

Run the service:

```bash
./mvnw -f demo/migration/camunda-service/pom.xml spring-boot:run
# Camunda Cockpit/Tasklist: http://localhost:8090  (login demo / demo)
```

Or containerised:

```bash
cd demo/migration/camunda-service && docker compose up --build
```

REST API:

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/cases` | Start a case from alert variables |
| `GET`  | `/api/tasks` | List alerts awaiting an analyst |
| `POST` | `/api/tasks/{id}/complete` | Analyst decision `{approved, analystNotes}` |
| `GET`  | `/api/cases/{id}` | Case status / outcome (from engine history) |

**The human approval gate UI:** open `migration/approval-ui/index.html` in a browser
(with the service running on 8090). Click **Seed sample alerts** — false positives
auto-close silently and only the genuine exceptions appear in the queue for approve /
override. Nothing is escalated or closed on an exception without an analyst acting.

---

## Suggested 30–40 min demo flow

1. **Frame it (2 min).** The problem is operations complexity, not model accuracy.
2. **Triage, live (12 min).** Point Devin at the alert batch + rules; show dozens of
   alerts triaged in parallel with reasoning attached; land on the disposition report
   and the ~67% auto-close rate.
3. **Migration, live (18 min).** Show the Pega export and BPMN; have Devin generate the
   Camunda + Java implementation against the standards, write the tests, and open a PR.
4. **Human gate + wrap (5 min).** Walk the approval-gate UI, approve one escalation and
   override another, and point at the audit trail. Mirrors Itaú (~70% auto-remediation)
   and the Barclays migration motion, at portfolio scale.
