package com.example.fraudapplication.service;

import com.example.fraudapplication.domain.enums.AlertName;
import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.impl.AlertGeneratorImpl;
import com.example.fraudapplication.service.impl.PingPongActivityServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Control tests for FR-PPA-003 (Ping-Pong Activity).
 *
 * Intent: flag four consecutive transactions alternating between exactly two services
 * (A, B, A, B) whose first and last transactions are at most 10 minutes apart (inclusive).
 */
class PingPongActivityServiceImplTest {

    private static final String USER = "user1";
    private static final Instant BASE = Instant.parse("2024-01-01T00:00:00Z");

    private final PingPongActivityService service =
            new PingPongActivityServiceImpl(new AlertGeneratorImpl());

    private static TransactionEvent event(String serviceId, long secondsFromBase) {
        return new TransactionEvent(BASE.plusSeconds(secondsFromBase), 100.00, USER, serviceId);
    }

    private static TransactionEvent event(String serviceId, long secondsFromBase, long millisFromBase) {
        return new TransactionEvent(BASE.plusSeconds(secondsFromBase).plusMillis(millisFromBase),
                100.00, USER, serviceId);
    }

    private List<Alert> run(TransactionEvent... events) {
        return service.checkPingPongActivity(new ArrayList<>(Arrays.asList(events)),
                new ArrayList<>(), USER);
    }

    private static long pingPongAlerts(List<Alert> alerts) {
        return alerts.stream()
                .filter(a -> AlertName.PING_PONG.getAlertName().equals(a.getAlertName()))
                .count();
    }

    @Test
    void bounceJustInsideWindowAlertsOnce() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 100), event("serviceA", 200),
                event("serviceB", 599));

        assertEquals(1, pingPongAlerts(alerts));
    }

    @Test
    void bounceExactlyAtWindowEdgeAlertsOnce() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 100), event("serviceA", 200),
                event("serviceB", 600));

        assertEquals(1, pingPongAlerts(alerts));
    }

    @Test
    void bounceJustOutsideWindowDoesNotAlert() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 100), event("serviceA", 200),
                event("serviceB", 601));

        assertEquals(0, pingPongAlerts(alerts));
    }

    @Test
    void subSecondOverrunOfTheWindowDoesNotAlert() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 100), event("serviceA", 200),
                event("serviceB", 600, 1));

        assertEquals(0, pingPongAlerts(alerts));
    }

    @Test
    void nonAlternatingSequenceDoesNotAlert() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 60), event("serviceC", 120),
                event("serviceB", 180));

        assertEquals(0, pingPongAlerts(alerts));
    }

    @Test
    void thirdBounceIsRequiredBeforeAlerting() {
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 60), event("serviceA", 120));

        assertEquals(0, pingPongAlerts(alerts));
    }

    @Test
    void bounceAtTheEndOfTheStreamIsStillDetected() {
        // adversarial: the bounce is padded with unrelated leading activity so that it lands on
        // the final transactions of the stream
        List<Alert> alerts = run(event("serviceZ", 0), event("serviceY", 10), event("serviceA", 20),
                event("serviceB", 30), event("serviceA", 40), event("serviceB", 50));

        assertEquals(1, pingPongAlerts(alerts));
    }

    @Test
    void bounceSplitByAnInterleavedThirdServiceEvadesTheRule() {
        // adversarial: a decoy transaction in a third service is injected between the bounces.
        // Documented residual risk: the pattern must be consecutive.
        List<Alert> alerts = run(event("serviceA", 0), event("serviceB", 60), event("serviceC", 90),
                event("serviceA", 120), event("serviceB", 180));

        assertEquals(0, pingPongAlerts(alerts));
    }

    @Test
    void outOfOrderEventsAreEvaluatedChronologically() {
        List<Alert> alerts = run(event("serviceB", 180), event("serviceA", 0), event("serviceB", 60),
                event("serviceA", 120));

        assertEquals(1, pingPongAlerts(alerts));
    }

    @Test
    void emptyTransactionListIsHandled() {
        assertEquals(0, pingPongAlerts(run()));
    }
}
