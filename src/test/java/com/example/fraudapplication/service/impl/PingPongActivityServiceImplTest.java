package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PingPongActivityServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private PingPongActivityServiceImpl pingPongActivityService;

    @Test
    void fraudDetected_alternatingBetweenTwoServices() {
        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName("Ping-Pong Activity Alert")
                .alertMessage("test")
                .alertTime("now")
                .build();
        when(alertGenerator.generatePingPongAlert("user1")).thenReturn(mockAlert);

        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(120), 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(180), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(240), 100.00, "user1", "serviceA"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generatePingPongAlert("user1");
    }

    @Test
    void noFraud_noAlternation() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(120), 100.00, "user1", "serviceC"));
        transactions.add(new TransactionEvent(base.plusSeconds(180), 100.00, "user1", "serviceD"));
        transactions.add(new TransactionEvent(base.plusSeconds(240), 100.00, "user1", "serviceE"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void noFraud_alternationBeyond10Minutes() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(700), 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(800), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(900), 100.00, "user1", "serviceA"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void multiplePingPongAlerts() {
        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName("Ping-Pong Activity Alert")
                .alertMessage("test")
                .alertTime("now")
                .build();
        when(alertGenerator.generatePingPongAlert("user1")).thenReturn(mockAlert);

        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(30), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(90), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(120), 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(150), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(180), 100.00, "user1", "serviceA"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.size() >= 2);
        verify(alertGenerator, atLeast(2)).generatePingPongAlert("user1");
    }

    @Test
    void minimumTransactions_only2_noAlert() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }
}
