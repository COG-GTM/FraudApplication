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
class MultipleServiceTransactionImplTest {

    @Mock
    private AlertGenerator alertGenerator;

    @InjectMocks
    private MultipleServiceTransactionImpl multipleServiceTransaction;

    @Test
    void testMoreThan3ServicesWithin5MinTriggersAlert() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("sA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(60)).amount(100.0).userID("user1").serviceID("sB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(120)).amount(100.0).userID("user1").serviceID("sC").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(180)).amount(100.0).userID("user1").serviceID("sD").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(240)).amount(100.0).userID("user1").serviceID("sE").build());

        Alert mockAlert = Alert.builder().userId("user1").alertName("Multiple Service Alert").alertMessage("msg").alertTime("time").build();
        when(alertGenerator.generateMultipleServiceAlert("user1")).thenReturn(mockAlert);

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generateMultipleServiceAlert("user1");
    }

    @Test
    void testLessThan4ServicesNoAlert() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("sA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(60)).amount(100.0).userID("user1").serviceID("sB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(120)).amount(100.0).userID("user1").serviceID("sC").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void testServicesOutside5MinWindow() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("sA").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(400)).amount(100.0).userID("user1").serviceID("sB").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(800)).amount(100.0).userID("user1").serviceID("sC").build());
        transactions.add(TransactionEvent.builder().timestamp(base.plusSeconds(1200)).amount(100.0).userID("user1").serviceID("sD").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void testSingleTransaction() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(TransactionEvent.builder().timestamp(base).amount(100.0).userID("user1").serviceID("sA").build());

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void testEmptyTransactionList() {
        List<TransactionEvent> transactions = new ArrayList<>();
        List<Alert> alerts = new ArrayList<>();

        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }
}
