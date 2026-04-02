package com.example.fraudapplication.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FraudDetectorEngineTest {

    @Test
    void testConstructorAndGetters() {
        FraudDetectorEngine engine = new FraudDetectorEngine();
        assertNotNull(engine.getUserTransactions());
        assertNotNull(engine.getAlerts());
        assertTrue(engine.getUserTransactions().isEmpty());
        assertTrue(engine.getAlerts().isEmpty());
    }

    @Test
    void testUserTransactionsMap() {
        FraudDetectorEngine engine = new FraudDetectorEngine();
        Map<String, List<TransactionEvent>> userTransactions = engine.getUserTransactions();

        List<TransactionEvent> events = new ArrayList<>();
        events.add(TransactionEvent.builder().amount(100.0).userID("user1").serviceID("sA").build());
        userTransactions.put("user1", events);

        assertEquals(1, engine.getUserTransactions().size());
        assertTrue(engine.getUserTransactions().containsKey("user1"));
    }

    @Test
    void testAlertsList() {
        FraudDetectorEngine engine = new FraudDetectorEngine();
        List<Alert> alerts = engine.getAlerts();

        alerts.add(Alert.builder().userId("user1").alertName("test").alertMessage("msg").alertTime("time").build());

        assertEquals(1, engine.getAlerts().size());
    }

    @Test
    void testToString() {
        FraudDetectorEngine engine = new FraudDetectorEngine();
        assertNotNull(engine.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        FraudDetectorEngine engine1 = new FraudDetectorEngine();
        FraudDetectorEngine engine2 = new FraudDetectorEngine();

        assertEquals(engine1, engine2);
        assertEquals(engine1.hashCode(), engine2.hashCode());
    }
}
