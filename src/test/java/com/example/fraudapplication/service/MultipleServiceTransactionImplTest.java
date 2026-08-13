package com.example.fraudapplication.service;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.impl.AlertGeneratorImpl;
import com.example.fraudapplication.service.impl.MultipleServiceTransactionImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Control tests for FR-MST-002 (Multiple Service Transactions).
 *
 * Intent: flag a user transacting in more than 3 distinct services within any 5-minute
 * sliding window (window boundary inclusive).
 */
class MultipleServiceTransactionImplTest {

    private static final String USER = "user1";
    private static final Instant BASE = Instant.parse("2024-01-01T00:00:00Z");

    private final MultipleServiceTransaction service =
            new MultipleServiceTransactionImpl(new AlertGeneratorImpl());

    private static TransactionEvent event(String serviceId, long secondsFromBase) {
        return new TransactionEvent(BASE.plusSeconds(secondsFromBase), 100.00, USER, serviceId);
    }

    private List<Alert> run(TransactionEvent... events) {
        return service.checkMultipleServiceTransactions(new ArrayList<>(Arrays.asList(events)),
                new ArrayList<>(), USER);
    }

    private static long multipleServiceAlerts(List<Alert> alerts) {
        return alerts.stream()
                .filter(a -> AlertName.MULTIPLE_SERVICE.getAlertName().equals(a.getAlertName()))
                .count();
    }

    @Test
    void threeDistinctServicesInWindowDoesNotAlert() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 30), event("serviceC", 60),
                event("serviceA", 90));

        assertEquals(0, multipleServiceAlerts(alerts));
    }

    @Test
    void fourthDistinctServiceExactlyAtWindowEdgeAlertsOnce() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 100), event("serviceC", 200),
                event("serviceD", 300));

        assertEquals(1, multipleServiceAlerts(alerts));
    }

    @Test
    void fourDistinctServicesWellInsideWindowAlertsOnce() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 10), event("serviceC", 20),
                event("serviceD", 30));

        assertEquals(1, multipleServiceAlerts(alerts));
    }

    @Test
    void fourthDistinctServiceJustOutsideWindowDoesNotAlert() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 100), event("serviceC", 200),
                event("serviceD", 301));

        assertEquals(0, multipleServiceAlerts(alerts));
    }

    @Test
    void repeatedUseOfThreeServicesDoesNotAlert() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 20), event("serviceC", 40),
                event("serviceA", 60), event("serviceB", 80), event("serviceC", 100));

        assertEquals(0, multipleServiceAlerts(alerts));
    }

    @Test
    void servicesSpreadAcrossSeparateWindowsEvadeTheRule() {
        // adversarial: 6 distinct services, never more than 3 inside any 5-minute window.
        // Documented residual risk of a 5-minute window; must not produce a false positive.
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 60), event("serviceC", 120),
                event("serviceD", 700), event("serviceE", 760), event("serviceF", 820));

        assertEquals(0, multipleServiceAlerts(alerts));
    }

    @Test
    void burstStraddlingAnEarlierTransactionIsStillDetected() {
        // adversarial: a decoy transaction is placed far ahead of the burst so that a window
        // anchored on the user's first transaction never sees the 4 distinct services
        List<Alert> alerts = run(event("serviceZ", 0), event("serviceA", 1000), event("serviceB", 1010),
                event("serviceC", 1020), event("serviceD", 1030));

        assertEquals(1, multipleServiceAlerts(alerts));
    }

    @Test
    void outOfOrderEventsAreEvaluatedChronologically() {
        List<Alert> alerts = run(event("serviceD", 30), event("serviceA", 0), event("serviceC", 20),
                event("serviceB", 10));

        assertEquals(1, multipleServiceAlerts(alerts));
    }

    @Test
    void emptyTransactionListIsHandled() {
        assertEquals(0, multipleServiceAlerts(run()));
    }
}
