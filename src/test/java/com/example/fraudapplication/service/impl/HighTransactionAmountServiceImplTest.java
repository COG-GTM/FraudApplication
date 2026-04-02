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
class HighTransactionAmountServiceImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private HighTransactionAmountServiceImpl highTransactionAmountService;

    @Test
    void testHighAmountTriggersAlert() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        // 4 normal transactions within 24 hours, average = 100
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(3600)).amount(100.0).userID("user1").serviceID("sA").build());
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(7200)).amount(100.0).userID("user1").serviceID("sB").build());
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(1800)).amount(100.0).userID("user1").serviceID("sC").build());
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(900)).amount(100.0).userID("user1").serviceID("sD").build());
        // This transaction is outside 24hrs so it won't affect the average,
        // but the check iterates ALL transactions. Its amount (10000) > 5 * avg(100) = 500.
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(48 * 3600)).amount(10000.0).userID("user1").serviceID("sE").build());

        Alert mockAlert = Alert.builder().userId("user1").alertName("High Transaction Alert").alertMessage("msg").alertTime("time").build();
        when(alertGenerator.generateHighTransactionAlert("user1")).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generateHighTransactionAlert("user1");
    }

    @Test
    void testNoHighAmountNoAlert() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(3600)).amount(100.0).userID("user1").serviceID("sA").build());
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(7200)).amount(110.0).userID("user1").serviceID("sB").build());
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(1800)).amount(105.0).userID("user1").serviceID("sC").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testEmptyTransactionList() {
        List<TransactionEvent> transactions = new ArrayList<>();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }

    @Test
    void testAllTransactionsOutside24Hours() {
        Instant now = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        // All transactions older than 24 hours
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(48 * 60 * 60)).amount(100.0).userID("user1").serviceID("sA").build());
        transactions.add(TransactionEvent.builder().timestamp(now.minusSeconds(48 * 60 * 60)).amount(200.0).userID("user1").serviceID("sB").build());

        List<Alert> alerts = new ArrayList<>();
        // With the bug fix, this should return safely without ArithmeticException
        List<Alert> result = highTransactionAmountService.checkHighAmountTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateHighTransactionAlert(anyString());
    }
}
