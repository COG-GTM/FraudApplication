# Fraud Rule Control Matrix

Scope: the three detection rules orchestrated by `FraudDetectorEngineServiceImpl`
(`src/main/java/com/example/fraudapplication/service/impl/FraudDetectorEngineServiceImpl.java:28-49`).

- **Before** = result of the tests in this PR run against the implementation at commit `5f67d94`
  (pre-change); reproduce with `git stash push -- src/main && mvn -B test`.
- **After** = result of `./mvnw clean test` at the head of this PR (33 tests, 0 failures, 0 errors).
- Every `file:line` below points at the head of this PR unless it is explicitly marked *(pre-change)*.

## 1. Rule inventory

| Rule ID | Intent | Thresholds / window (constants) | Inputs read | Enforced at |
| --- | --- | --- | --- | --- |
| FR-HTA-001 High Transaction Amount | Flag a transaction whose amount is at least 5x the mean amount of the same user's *other* transactions in the trailing 24 hours. | `HIGH_AMOUNT_MULTIPLIER = 5.0` (`HighTransactionAmountServiceImpl.java:21`), `BASELINE_WINDOW_SECONDS = 24*60*60` (`HighTransactionAmountServiceImpl.java:18`) | `TransactionEvent.amount`, `TransactionEvent.timestamp`, `userId` | `src/main/java/com/example/fraudapplication/service/impl/HighTransactionAmountServiceImpl.java:47` (pre-change: same file `:34`) |
| FR-MST-002 Multiple Service Transactions | Flag a user transacting in more than 3 distinct services within any 5-minute sliding window (window boundary inclusive, exact to the nanosecond). | `DISTINCT_SERVICE_THRESHOLD = 3` (exclusive) (`MultipleServiceTransactionImpl.java:28`), `DISTINCT_SERVICE_WINDOW_SECONDS = 5*60` (`MultipleServiceTransactionImpl.java:23`) | `TransactionEvent.serviceID`, `TransactionEvent.timestamp`, `userId` | `src/main/java/com/example/fraudapplication/service/impl/MultipleServiceTransactionImpl.java:57` (window eviction `:47-50`; pre-change: same file `:42`) |
| FR-PPA-003 Ping-Pong Activity | Flag four consecutive transactions alternating between exactly two services (A,B,A,B) whose first and last transactions are at most 10 minutes apart (inclusive, exact to the nanosecond). | `PING_PONG_SEQUENCE_LENGTH = 4` (`PingPongActivityServiceImpl.java:24`), `PING_PONG_WINDOW_SECONDS = 10*60` (`PingPongActivityServiceImpl.java:19`) | `TransactionEvent.serviceID`, `TransactionEvent.timestamp`, `userId` | `src/main/java/com/example/fraudapplication/service/impl/PingPongActivityServiceImpl.java:53` (pattern `:47-51`; pre-change: same file `:58-60`) |

Alert emission for all three rules: `AlertGeneratorImpl.java:17`, `:27`, `:37`; alert text is fixed by
`domain/enums/AlertName.java:10-17` and is unchanged by this PR. The `/fraud/download` CSV output
(`controller/FraudDetectionController.java`, `domain/utils/ListToCsv.java`) is untouched.

## 2. Control test matrix

### FR-HTA-001 — High Transaction Amount
Tests: `src/test/java/com/example/fraudapplication/service/HighTransactionAmountServiceImplTest.java`

| Case | Test (file:line) | Before | After |
| --- | --- | --- | --- |
| Just below threshold (baseline 100.00, amount 499.99) | `justBelowThresholdDoesNotAlert` `:47` | PASS | PASS |
| Exactly at threshold (amount 500.00 = 5x baseline) | `exactlyAtThresholdAlerts` `:56` | FAIL (expected 1 alert, got 0) | PASS |
| Just above threshold (amount 500.01) | `justAboveThresholdAlerts` `:64` | FAIL (expected 1 alert, got 0) | PASS |
| Negative case — ordinary spread of amounts must not flag | `ordinaryActivityDoesNotAlert` `:72` | PASS | PASS |
| Baseline window — transaction 25h old is neither scored nor part of the baseline | `baselineIgnoresTransactionsOlderThan24Hours` `:80` | FAIL (expected 0 alerts, got 1) | PASS |
| Adversarial — `NaN` amount poisons the mean and silently disables the rule | `nonFiniteAmountCannotDisableTheRule` `:90` | FAIL (expected >=1 alert, got 0) | PASS |
| Adversarial — one large amount split into two sub-threshold amounts (documented residual risk) | `splittingOneLargeAmountIntoSubThresholdTransactionsEvadesTheRule` `:100` | PASS | PASS |
| Zero and negative amounts excluded from scoring and from the baseline | `nonPositiveAmountsAreExcludedFromScoringAndBaseline` `:110` | FAIL (expected 1 alert, got 5 — the refund dragged the mean to 0 so every transaction flagged) | PASS |
| Empty transaction list | `emptyTransactionListIsHandled` `:120` | PASS | PASS |

### FR-MST-002 — Multiple Service Transactions
Tests: `src/test/java/com/example/fraudapplication/service/MultipleServiceTransactionImplTest.java`

| Case | Test (file:line) | Before | After |
| --- | --- | --- | --- |
| Just below threshold — 3 distinct services in window | `threeDistinctServicesInWindowDoesNotAlert` `:47` | PASS | PASS |
| Exactly at window edge — 4th distinct service at t+300s | `fourthDistinctServiceExactlyAtWindowEdgeAlertsOnce` `:55` | PASS | PASS |
| Above threshold — 4 distinct services within 30s | `fourDistinctServicesWellInsideWindowAlertsOnce` `:63` | PASS | PASS |
| Just outside window — 4th distinct service at t+301s | `fourthDistinctServiceJustOutsideWindowDoesNotAlert` `:71` | PASS | PASS |
| Negative case — 3 services reused repeatedly | `repeatedUseOfThreeServicesDoesNotAlert` `:79` | PASS | PASS |
| Adversarial — 6 services spread so no window holds 4 (documented residual risk; must not false-positive) | `servicesSpreadAcrossSeparateWindowsEvadeTheRule` `:87` | FAIL (expected 0 alerts, got 2) | PASS |
| Adversarial — decoy transaction long before the burst, so an anchor-based window misses it | `burstStraddlingAnEarlierTransactionIsStillDetected` `:97` | PASS | PASS |
| Out-of-order delivery evaluated chronologically | `outOfOrderEventsAreEvaluatedChronologically` `:107` | PASS | PASS |
| Empty transaction list | `emptyTransactionListIsHandled` `:115` | FAIL (`IndexOutOfBoundsException`) | PASS |

### FR-PPA-003 — Ping-Pong Activity
Tests: `src/test/java/com/example/fraudapplication/service/PingPongActivityServiceImplTest.java`

| Case | Test (file:line) | Before | After |
| --- | --- | --- | --- |
| Just below window — bounce spanning 599s | `bounceJustInsideWindowAlertsOnce` `:52` | FAIL (expected 1 alert, got 0) | PASS |
| Exactly at window edge — bounce spanning 600s | `bounceExactlyAtWindowEdgeAlertsOnce` `:60` | FAIL (expected 1 alert, got 0) | PASS |
| Just outside window — bounce spanning 601s | `bounceJustOutsideWindowDoesNotAlert` `:68` | PASS | PASS |
| Sub-second overrun — bounce spanning 600.001s | `subSecondOverrunOfTheWindowDoesNotAlert` `:76` | FAIL (expected 0 alerts, got 1 against the first revision of this PR, which truncated sub-second parts; the pre-change implementation never alerted at all) | PASS |
| Negative case — A,B,C,B is not a bounce | `nonAlternatingSequenceDoesNotAlert` `:84` | PASS | PASS |
| Negative case — only 3 transactions (A,B,A) | `thirdBounceIsRequiredBeforeAlerting` `:92` | PASS | PASS |
| Adversarial — bounce padded so it lands on the final transactions of the stream | `bounceAtTheEndOfTheStreamIsStillDetected` `:99` | FAIL (expected 1 alert, got 0) | PASS |
| Adversarial — third-service decoy injected between bounces (documented residual risk) | `bounceSplitByAnInterleavedThirdServiceEvadesTheRule` `:109` | PASS | PASS |
| Out-of-order delivery evaluated chronologically | `outOfOrderEventsAreEvaluatedChronologically` `:119` | FAIL (expected 1 alert, got 0) | PASS |
| Empty transaction list | `emptyTransactionListIsHandled` `:127` | FAIL (`IndexOutOfBoundsException`) | PASS |

## 3. Implementation defects corrected (implementation changed, tests not weakened)

| Rule | Defect (pre-change file:line) | Corrected behaviour (file:line) |
| --- | --- | --- |
| FR-HTA-001 | Strict `>` on an inclusive threshold: a transaction at exactly 5x the baseline did not alert (`HighTransactionAmountServiceImpl.java:34` pre-change) | `>=` comparison (`HighTransactionAmountServiceImpl.java:47`) |
| FR-HTA-001 | The scored transaction was included in its own baseline, so the threshold was unreachable for small sets and shifted for large ones (`:22-34` pre-change) | Baseline is the mean of the user's *other* in-window transactions (`:43-47`) |
| FR-HTA-001 | The 24h filter applied to the baseline only; the alert loop scored *all* transactions, including ones outside the window (`:33` pre-change) | Window filter applied once, to both baseline and scoring (`:32-37`) |
| FR-HTA-001 | Unvalidated amounts: a single `NaN` made every comparison false and disabled the rule; zero in-window transactions divided by zero (`:22-34` pre-change) | Non-finite and non-positive amounts excluded; at least 2 valid in-window transactions required (`:36-41`, `:54-56`) |
| FR-MST-002, FR-PPA-003 | Window comparison via `Duration.getSeconds()` truncated sub-second parts, so a 600.9s bounce counted as inside a 600s window | Windows compared as `Duration` values, exact to the nanosecond (`MultipleServiceTransactionImpl.java:47-48`, `PingPongActivityServiceImpl.java:50-51`) |
| FR-MST-002 | Window anchored on a mutable "first transaction" instead of sliding, and only the anchor's service was ever evicted, so services from long-expired windows kept accumulating (false positives) (`:26-41` pre-change) | True sliding window with eviction of every event older than 5 minutes (`:44-55`) |
| FR-MST-002 | Once breached, one alert was appended for *every* remaining transaction (`:42-44` pre-change) | One alert per breach, window reset afterwards (`:57-60`) |
| FR-MST-002 | `transactions.get(0)` on an empty list threw `IndexOutOfBoundsException`; unsorted input evaluated in arrival order (`:26` pre-change) | Empty/blank input guarded and events sorted chronologically (`:35-42`) |
| FR-PPA-003 | Detection loop ran `index+2 .. size-2`, so a bounce at the start or end of the stream was never examined (`:45` pre-change) | Every 4-transaction sliding sequence is examined (`:40-45`) |
| FR-PPA-003 | Two-service state map could retain stale services, alerting on non-alternating sequences (`:58-69` pre-change) | Explicit A,B,A,B pattern check on consecutive transactions (`:47-49`) |
| FR-PPA-003 | `transactions.get(0)` on an empty list threw `IndexOutOfBoundsException`; unsorted input evaluated in arrival order (`:27` pre-change) | Short/blank input guarded and events sorted chronologically (`:31-38`) |
| Engine | `checkHighTransactionFraud` / `checkPingPongFraud` / `checkMultipleServiceFraud` passed the shared alert list to the rule *and* re-added the returned list to itself, duplicating every alert (`FraudDetectorEngineServiceImpl.java:67,89,111` pre-change) | Rule appends to the shared list once (`FraudDetectorEngineServiceImpl.java:67,89,111`) |

## 4. Residual risks (accepted, covered by a passing test)

| Rule | Residual risk | Evidence |
| --- | --- | --- |
| FR-HTA-001 | Structuring: splitting one large amount into several sub-threshold amounts is not detected; the rule scores single amounts only. | `HighTransactionAmountServiceImplTest.splittingOneLargeAmountIntoSubThresholdTransactionsEvadesTheRule:100` |
| FR-MST-002 | Pacing: using at most 3 distinct services per 5-minute window is not detected; inherent to the stated window. | `MultipleServiceTransactionImplTest.servicesSpreadAcrossSeparateWindowsEvadeTheRule:87` |
| FR-PPA-003 | Interleaving: a decoy transaction in a third service breaks the consecutive A,B,A,B pattern. | `PingPongActivityServiceImplTest.bounceSplitByAnInterleavedThirdServiceEvadesTheRule:109` |
| FR-HTA-001 | Low-volume users: with exactly two in-window transactions the baseline is the single other amount, so any pair with a >=5x ratio alerts. Raising the minimum sample size is a policy decision, not a code defect, and is left open. | `HighTransactionAmountServiceImpl.java:39` |
| All rules | Rules are evaluated per request batch (`FraudDetectorEngineServiceImpl.java:37-45`); the accumulated per-user history in `FraudDetectorEngine` is written but never read, and the shared alert list is unsynchronised and unbounded. Out of scope for this control review. | `FraudDetectorEngineServiceImpl.java:35-45`, `domain/model/FraudDetectorEngine.java:13-14` |
