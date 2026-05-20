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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private HighTransactionAmountServiceImpl highTransactionAmountService;

    @Test
    void noFraudDetected_allTransactionsWithinNormalRange() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(60), 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(120), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(180), 100.00, "user1", "serviceC"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void fraudDetected_transactionExceeds5xAverage() {
        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName("High Transaction Alert")
                .alertMessage("test")
                .alertTime("now")
                .build();
        when(alertGenerator.generateHighTransactionAlert("user1")).thenReturn(mockAlert);

        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(60), 1.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(120), 1.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(180), 1.00, "user1", "serviceC"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(240), 1.00, "user1", "serviceD"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(300), 1.00, "user1", "serviceE"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(360), 100.00, "user1", "serviceF"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertFalse(result.isEmpty());
        assertEquals("High Transaction Alert", result.get(0).getAlertName());
        verify(alertGenerator, atLeastOnce()).generateHighTransactionAlert("user1");
    }

    @Test
    void emptyTransactionList_noExceptionsAndNoAlerts() {
        List<TransactionEvent> transactions = Collections.emptyList();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void allTransactionsOutside24hWindow_producesNaN_noAlerts() {
        // total24HoursTransaction will be 0, causing double division by zero (NaN)
        // In Java, double / 0 = NaN (no ArithmeticException), and amount > NaN is always false
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(25 * 60 * 60), 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(26 * 60 * 60), 200.00, "user1", "serviceB"));

        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void singleTransaction_amountEqualsAverage_noFraud() {
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(Instant.now().minusSeconds(60), 100.00, "user1", "serviceA"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }
}
