package com.example.fraudapplication.service;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.impl.AlertGeneratorImpl;
import com.example.fraudapplication.service.impl.HighTransactionAmountServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Control tests for FR-HTA-001 (High Transaction Amount).
 *
 * Intent: flag a transaction whose amount is at least 5x the mean of the user's other
 * transactions in the trailing 24 hours.
 */
class HighTransactionAmountServiceImplTest {

    private static final String USER = "user1";

    private final HighTransactionAmountService service =
            new HighTransactionAmountServiceImpl(new AlertGeneratorImpl());

    private static TransactionEvent event(double amount, long secondsAgo) {
        return new TransactionEvent(Instant.now().minusSeconds(secondsAgo), amount, USER, "serviceA");
    }

    private List<Alert> run(TransactionEvent... events) {
        return service.checkHighAmountTransactions(new ArrayList<>(Arrays.asList(events)),
                new ArrayList<>(), USER);
    }

    private static long highTransactionAlerts(List<Alert> alerts) {
        return alerts.stream()
                .filter(a -> AlertName.HIGH_TRANSACTION.getAlertName().equals(a.getAlertName()))
                .count();
    }

    @Test
    void justBelowThresholdDoesNotAlert() {
        // baseline mean of the other transactions = 100.00, threshold = 500.00
        List<Alert> alerts = run(event(100.00, 60), event(100.00, 50), event(100.00, 40),
                event(100.00, 30), event(499.99, 10));

        assertEquals(0, highTransactionAlerts(alerts));
    }

    @Test
    void exactlyAtThresholdAlerts() {
        List<Alert> alerts = run(event(100.00, 60), event(100.00, 50), event(100.00, 40),
                event(100.00, 30), event(500.00, 10));

        assertEquals(1, highTransactionAlerts(alerts));
    }

    @Test
    void justAboveThresholdAlerts() {
        List<Alert> alerts = run(event(100.00, 60), event(100.00, 50), event(100.00, 40),
                event(100.00, 30), event(500.01, 10));

        assertEquals(1, highTransactionAlerts(alerts));
    }

    @Test
    void ordinaryActivityDoesNotAlert() {
        List<Alert> alerts = run(event(100.00, 60), event(120.00, 50), event(90.00, 40),
                event(110.00, 30), event(105.00, 10));

        assertEquals(0, highTransactionAlerts(alerts));
    }

    @Test
    void baselineIgnoresTransactionsOlderThan24Hours() {
        // the 10 000.00 transaction is outside the window: it must neither be scored nor
        // inflate the baseline used for the in-window transactions
        List<Alert> alerts = run(event(10_000.00, 25 * 60 * 60), event(100.00, 60),
                event(100.00, 50), event(100.00, 40), event(120.00, 10));

        assertEquals(0, highTransactionAlerts(alerts));
    }

    @Test
    void nonFiniteAmountCannotDisableTheRule() {
        // adversarial: a single NaN amount poisons a naive mean so that every comparison is
        // false, silently disabling the control for the genuinely high transaction
        List<Alert> alerts = run(event(100.00, 90), event(100.00, 80), event(100.00, 70),
                event(100.00, 60), event(Double.NaN, 50), event(5_000.00, 10));

        assertTrue(highTransactionAlerts(alerts) >= 1);
    }

    @Test
    void splittingOneLargeAmountIntoSubThresholdTransactionsEvadesTheRule() {
        // adversarial: 1 000.00 would breach a 500.00 threshold, so it is split into two
        // 400.00 transactions. Documented residual risk: this rule scores single amounts only.
        List<Alert> alerts = run(event(100.00, 60), event(100.00, 50), event(100.00, 40),
                event(100.00, 30), event(400.00, 20), event(400.00, 10));

        assertEquals(0, highTransactionAlerts(alerts));
    }

    @Test
    void nonPositiveAmountsAreExcludedFromScoringAndBaseline() {
        // refunds/zero-value records are not "high amount" candidates and must not drag the
        // baseline down; the 500.00 transaction is still scored against the 100.00 baseline
        List<Alert> alerts = run(event(100.00, 90), event(100.00, 80), event(100.00, 70),
                event(100.00, 60), event(-900.00, 50), event(0.00, 40), event(500.00, 10));

        assertEquals(1, highTransactionAlerts(alerts));
    }

    @Test
    void emptyTransactionListIsHandled() {
        assertEquals(0, highTransactionAlerts(run()));
    }
}
