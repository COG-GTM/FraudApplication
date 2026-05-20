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
    void fraudDetected_fourDistinctServicesWithin5Minutes() {
        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName("Multiple Service Alert")
                .alertMessage("test")
                .alertTime("now")
                .build();
        when(alertGenerator.generateMultipleServiceAlert("user1")).thenReturn(mockAlert);

        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(120), 100.00, "user1", "serviceC"));
        transactions.add(new TransactionEvent(base.plusSeconds(180), 100.00, "user1", "serviceD"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertFalse(result.isEmpty());
        verify(alertGenerator, atLeastOnce()).generateMultipleServiceAlert("user1");
    }

    @Test
    void noFraud_only3ServicesWithin5Minutes() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(120), 100.00, "user1", "serviceC"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void noFraud_servicesSpreadBeyond5Minutes() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(120), 100.00, "user1", "serviceC"));
        transactions.add(new TransactionEvent(base.plusSeconds(400), 100.00, "user1", "serviceD"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }

    @Test
    void multipleAlerts_conditionTriggeredMultipleTimes() {
        Alert mockAlert = Alert.builder()
                .userId("user1")
                .alertName("Multiple Service Alert")
                .alertMessage("test")
                .alertTime("now")
                .build();
        when(alertGenerator.generateMultipleServiceAlert("user1")).thenReturn(mockAlert);

        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));
        transactions.add(new TransactionEvent(base.plusSeconds(60), 100.00, "user1", "serviceB"));
        transactions.add(new TransactionEvent(base.plusSeconds(120), 100.00, "user1", "serviceC"));
        transactions.add(new TransactionEvent(base.plusSeconds(180), 100.00, "user1", "serviceD"));
        transactions.add(new TransactionEvent(base.plusSeconds(240), 100.00, "user1", "serviceE"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.size() >= 2);
        verify(alertGenerator, atLeast(2)).generateMultipleServiceAlert("user1");
    }

    @Test
    void singleTransaction_noAlertAndNoException() {
        Instant base = Instant.now();
        List<TransactionEvent> transactions = new ArrayList<>();
        transactions.add(new TransactionEvent(base, 100.00, "user1", "serviceA"));

        List<Alert> alerts = new ArrayList<>();
        List<Alert> result = multipleServiceTransaction.checkMultipleServiceTransactions(transactions, alerts, "user1");

        assertTrue(result.isEmpty());
        verify(alertGenerator, never()).generateMultipleServiceAlert(anyString());
    }
}
