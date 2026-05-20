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
import java.util.Collections;
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

    private List<TransactionEvent> createEventsForUser(String userId) {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(new TransactionEvent(Instant.now().minusSeconds(60), 100.00, userId, "serviceA"));
        events.add(new TransactionEvent(Instant.now().minusSeconds(30), 200.00, userId, "serviceB"));
        return events;
    }

    @Test
    void checkAllFraudActivities_allSubServicesInvoked() {
        List<TransactionEvent> events = createEventsForUser("user1");

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
    }

    @Test
    void checkAllFraudActivities_alertsAggregated() {
        List<TransactionEvent> events = createEventsForUser("user1");

        Alert alert = Alert.builder().userId("user1").alertName("Test").build();
        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> {
                    List<Alert> alerts = inv.getArgument(1);
                    alerts.add(alert);
                    return alerts;
                });
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void checkHighTransactionFraud_onlyHighTransactionServiceCalled() {
        List<TransactionEvent> events = createEventsForUser("user1");

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkHighTransactionFraud(events);

        assertNotNull(result);
        verify(highTransactionAmountService).checkHighAmountTransactions(anyList(), anyList(), eq("user1"));
        verifyNoInteractions(pingPongActivityService);
        verifyNoInteractions(multipleServiceTransaction);
    }

    @Test
    void checkHighTransactionFraud_alertsReturned() {
        List<TransactionEvent> events = createEventsForUser("user1");

        Alert alert = Alert.builder().userId("user1").alertName("High Transaction Alert").build();
        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> {
                    List<Alert> alerts = inv.getArgument(1);
                    alerts.add(alert);
                    return alerts;
                });

        List<Alert> result = fraudDetectorEngineService.checkHighTransactionFraud(events);

        assertFalse(result.isEmpty());
    }

    @Test
    void checkPingPongFraud_onlyPingPongServiceCalled() {
        List<TransactionEvent> events = createEventsForUser("user1");

        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkPingPongFraud(events);

        assertNotNull(result);
        verify(pingPongActivityService).checkPingPongActivity(anyList(), anyList(), eq("user1"));
        verifyNoInteractions(highTransactionAmountService);
        verifyNoInteractions(multipleServiceTransaction);
    }

    @Test
    void checkPingPongFraud_alertsReturned() {
        List<TransactionEvent> events = createEventsForUser("user1");

        Alert alert = Alert.builder().userId("user1").alertName("Ping-Pong Activity Alert").build();
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> {
                    List<Alert> alerts = inv.getArgument(1);
                    alerts.add(alert);
                    return alerts;
                });

        List<Alert> result = fraudDetectorEngineService.checkPingPongFraud(events);

        assertFalse(result.isEmpty());
    }

    @Test
    void checkMultipleServiceFraud_onlyMultipleServiceCalled() {
        List<TransactionEvent> events = createEventsForUser("user1");

        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> inv.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkMultipleServiceFraud(events);

        assertNotNull(result);
        verify(multipleServiceTransaction).checkMultipleServiceTransactions(anyList(), anyList(), eq("user1"));
        verifyNoInteractions(highTransactionAmountService);
        verifyNoInteractions(pingPongActivityService);
    }

    @Test
    void checkMultipleServiceFraud_alertsReturned() {
        List<TransactionEvent> events = createEventsForUser("user1");

        Alert alert = Alert.builder().userId("user1").alertName("Multiple Service Alert").build();
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), eq("user1")))
                .thenAnswer(inv -> {
                    List<Alert> alerts = inv.getArgument(1);
                    alerts.add(alert);
                    return alerts;
                });

        List<Alert> result = fraudDetectorEngineService.checkMultipleServiceFraud(events);

        assertFalse(result.isEmpty());
    }

    @Test
    void multipleUsers_eachSubServiceCalledPerUser() {
        List<TransactionEvent> events = new ArrayList<>();
        events.addAll(createEventsForUser("user1"));
        events.addAll(createEventsForUser("user2"));

        when(highTransactionAmountService.checkHighAmountTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(pingPongActivityService.checkPingPongActivity(anyList(), anyList(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(multipleServiceTransaction.checkMultipleServiceTransactions(anyList(), anyList(), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        verify(highTransactionAmountService, times(2)).checkHighAmountTransactions(anyList(), anyList(), anyString());
        verify(pingPongActivityService, times(2)).checkPingPongActivity(anyList(), anyList(), anyString());
        verify(multipleServiceTransaction, times(2)).checkMultipleServiceTransactions(anyList(), anyList(), anyString());
    }

    @Test
    void emptyEventsList_noExceptionsAndEmptyAlerts() {
        List<TransactionEvent> events = Collections.emptyList();

        List<Alert> result = fraudDetectorEngineService.checkAllFraudActivities(events);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(highTransactionAmountService);
        verifyNoInteractions(pingPongActivityService);
        verifyNoInteractions(multipleServiceTransaction);
    }
}
