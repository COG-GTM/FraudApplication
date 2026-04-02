package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.HighTransactionAmountService;
import com.example.fraudapplication.service.MultipleServiceTransaction;
import com.example.fraudapplication.service.PingPongActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectorEngineServiceImplTest {

    @Mock
    private HighTransactionAmountService highTransactionAmountService;

    @Mock
    private MultipleServiceTransaction multipleServiceTransaction;

    @Mock
    private PingPongActivityService pingPongActivityService;

    @InjectMocks
    private FraudDetectorEngineServiceImpl fraudDetectorEngineService;

    private TransactionEvent createEvent(String userId, String serviceId) {
        return TransactionEvent.builder()
                .timestamp(Instant.now())
                .amount(100.0)
                .userID(userId)
                .serviceID(serviceId)
                .build();
    }

    @Test
    void testCheckAllFraudActivities_singleUser() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));
        events.add(createEvent("user1", "sB"));

        Alert mockAlert = Alert.builder().userId("user1").alertName("alert").alertMessage("msg").alertTime("time").build();

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(invocation -> {
                    List<Alert> alerts = invocation.getArgument(1);
                    alerts.add(mockAlert);
                    return alerts;
                });
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), eq("user1")))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertFalse(result.isEmpty());
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
    }

    @Test
    void testCheckAllFraudActivities_multipleUsers() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));
        events.add(createEvent("user2", "sB"));

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user2"));
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user2"));
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user2"));
    }

    @Test
    void testCheckAllFraudActivities_emptyList() {
        List<TransactionEvent> events = new ArrayList<>();

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        verifyNoInteractions(highTransactionAmountService);
        verifyNoInteractions(pingPongActivityService);
        verifyNoInteractions(multipleServiceTransaction);
    }

    @Test
    void testCheckHighTransactionFraud() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));

        Alert mockAlert = Alert.builder().userId("user1").alertName("High Transaction Alert").alertMessage("msg").alertTime("time").build();
        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(invocation -> {
                    List<Alert> alerts = invocation.getArgument(1);
                    alerts.add(mockAlert);
                    return alerts;
                });

        List<Alert> result = fraudDetectorEngineService.checkHighTransactionFraud(events);

        assertFalse(result.isEmpty());
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verifyNoInteractions(pingPongActivityService);
        verifyNoInteractions(multipleServiceTransaction);
    }

    @Test
    void testCheckHighTransactionFraud_multipleUsers() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));
        events.add(createEvent("user2", "sB"));

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkHighTransactionFraud(events);

        assertNotNull(result);
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user2"));
    }

    @Test
    void testCheckPingPongFraud() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));

        Alert mockAlert = Alert.builder().userId("user1").alertName("Ping-Pong Activity Alert").alertMessage("msg").alertTime("time").build();
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), eq("user1")))
                .thenAnswer(invocation -> {
                    List<Alert> alerts = invocation.getArgument(1);
                    alerts.add(mockAlert);
                    return alerts;
                });

        List<Alert> result = fraudDetectorEngineService.checkPingPongFraud(events);

        assertFalse(result.isEmpty());
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
        verifyNoInteractions(highTransactionAmountService);
        verifyNoInteractions(multipleServiceTransaction);
    }

    @Test
    void testCheckPingPongFraud_multipleUsers() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));
        events.add(createEvent("user2", "sB"));

        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkPingPongFraud(events);

        assertNotNull(result);
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user2"));
    }

    @Test
    void testCheckMultipleServiceFraud() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));

        Alert mockAlert = Alert.builder().userId("user1").alertName("Multiple Service Alert").alertMessage("msg").alertTime("time").build();
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(invocation -> {
                    List<Alert> alerts = invocation.getArgument(1);
                    alerts.add(mockAlert);
                    return alerts;
                });

        List<Alert> result = fraudDetectorEngineService.checkMultipleServiceFraud(events);

        assertFalse(result.isEmpty());
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
        verifyNoInteractions(highTransactionAmountService);
        verifyNoInteractions(pingPongActivityService);
    }

    @Test
    void testCheckMultipleServiceFraud_multipleUsers() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(createEvent("user1", "sA"));
        events.add(createEvent("user2", "sB"));

        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkMultipleServiceFraud(events);

        assertNotNull(result);
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user2"));
    }
}
