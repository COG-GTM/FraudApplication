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
    void testPingPongPatternTriggersAlert() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        // Alternating pattern: A, B, A, B, A — need 5 transactions for the algorithm's second loop
        // First loop finds A(0), B(1) → index=1. Second loop: j=3..3 (size-1=4), checks j-1=2(A) vs j=3(B)
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(60)).amount(100.0).userID("user1").serviceID("serviceB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(120)).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(180)).amount(100.0).userID("user1").serviceID("serviceB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(240)).amount(100.0).userID("user1").serviceID("serviceA").build());

        Alert mockAlert = Alert.builder().userId("user1").alertName("Ping-Pong Activity Alert").alertMessage("msg").alertTime("time").build();
        when(alertGenerator.generatePingPongAlert("user1")).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generatePingPongAlert("user1");
    }

    @Test
    void testNoPingPongPattern() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        // Different services, no back-and-forth
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(60)).amount(100.0).userID("user1").serviceID("serviceB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(120)).amount(100.0).userID("user1").serviceID("serviceC").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(180)).amount(100.0).userID("user1").serviceID("serviceD").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testPingPongOutside10MinWindow() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        // First 2 services within 10 min so the map gets populated,
        // but subsequent alternating transactions are > 10 min from the first, so no alert
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(60)).amount(100.0).userID("user1").serviceID("serviceB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(700)).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(1400)).amount(100.0).userID("user1").serviceID("serviceB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(2100)).amount(100.0).userID("user1").serviceID("serviceA").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testOnlyTwoTransactions() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(60)).amount(100.0).userID("user1").serviceID("serviceB").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testSingleTransaction() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("serviceA").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testAllSameServiceNoException() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        // All transactions to the same service — last2ServiceMap never reaches size 2
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(60)).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(120)).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(180)).amount(100.0).userID("user1").serviceID("serviceA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(240)).amount(100.0).userID("user1").serviceID("serviceA").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }

    @Test
    void testEmptyTransactionList() {
        List<TransactionEvent> transactions = new ArrayList<>();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = pingPongActivityService.checkPingPongActivity(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generatePingPongAlert(anyString());
    }
}
