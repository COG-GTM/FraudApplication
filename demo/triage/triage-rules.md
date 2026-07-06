# Fraud Alert Triage Rules

These are the first-pass triage rules the fraud operations team applies to every
alert produced by the detection engine. The goal is to remove the false-positive
load from human analysts and to make sure that **only genuine exceptions** reach
an investigator, while every decision keeps a written rationale for audit.

Each alert is assigned exactly one **disposition**:

| Disposition | Meaning | Who acts next |
|-------------|---------|---------------|
| `AUTO_CLOSE_FALSE_POSITIVE` | Benign activity that matched a heuristic. Closed automatically with rationale. | Nobody — logged for audit |
| `INVESTIGATE` | Not obviously benign, no financial-crime red flags. Needs analyst review. | Fraud analyst |
| `ESCALATE_SAR` | Financial-crime red flag present. Route to the SAR / EDD queue. | Financial-crime investigator |

## Alert context fields

Every alert carries an enrichment `context` block:

| Field | Type | Notes |
|-------|------|-------|
| `kycVerified` | boolean | Customer KYC is current |
| `sanctionsHit` | boolean | Counterparty matched a sanctions list |
| `pepMatch` | boolean | Counterparty is a politically exposed person |
| `whitelistedCounterparty` | boolean | Counterparty on the customer's approved list |
| `counterpartyType` | enum | `RECURRING_PAYEE`, `MERCHANT`, `INTERNAL_ACCOUNT`, `EXTERNAL_ACCOUNT`, `NEW_PAYEE` |
| `amountVsAvgRatio` | number | Amount as a multiple of the 24h rolling average |
| `distinctServices` | number | Distinct services touched in the window |
| `accountType` | enum | `PERSONAL`, `BUSINESS` |
| `structuringPattern` | boolean | Multiple sub-threshold transfers detected |
| `geoConsistent` | boolean | Transaction geography consistent with the profile |
| `priorSarCount` | number | Suspicious Activity Reports already filed on the customer |
| `customerTenureDays` | number | Age of the customer relationship |

## Decision rules (first match wins)

Rules are evaluated top to bottom; the first one that matches sets the disposition.

1. **Financial-crime red flags → `ESCALATE_SAR`**
   If any of the following is true, escalate:
   - `sanctionsHit == true`
   - `pepMatch == true`
   - `structuringPattern == true`
   - `priorSarCount >= 1`

2. **High model risk → `ESCALATE_SAR`**
   If `riskScore >= 80`, escalate.

3. **Known-good patterns → `AUTO_CLOSE_FALSE_POSITIVE`** (only when `riskScore < 60`)
   - `HIGH_TRANSACTION` **and** `kycVerified` **and**
     (`counterpartyType in {RECURRING_PAYEE, MERCHANT}` **or** `whitelistedCounterparty == true`)
   - `PING_PONG` **and** `counterpartyType == INTERNAL_ACCOUNT`
   - `MULTIPLE_SERVICE` **and** `accountType == BUSINESS`

4. **Everything else → `INVESTIGATE`**

## Rationale requirement

For every disposition the triage engine records a plain-English rationale citing
the specific fields that drove the decision, plus the rule number that fired.
This rationale is what the room sees attached to each triaged alert and is what
lands in the audit trail.

## Expected outcome on the sample dataset

The 24-alert sample in `alerts/alerts.json` is engineered so that a correct
application of these rules yields roughly a **2/3 auto-close rate** — mirroring
the real-world result where the majority of fraud alerts are false positives and
only genuine exceptions consume analyst time.

| Disposition | Count |
|-------------|-------|
| `AUTO_CLOSE_FALSE_POSITIVE` | 16 |
| `INVESTIGATE` | 5 |
| `ESCALATE_SAR` | 3 |
