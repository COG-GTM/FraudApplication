package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.FraudDetectorEngine;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.FraudDetectorEngineService;
import com.example.fraudapplication.service.HighTransactionAmountService;
import com.example.fraudapplication.service.MultipleServiceTransaction;
import com.example.fraudapplication.service.PingPongActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FraudDetectorEngineServiceImpl implements FraudDetectorEngineService {

    private final FraudDetectorEngine fraudDetectorEngine = new FraudDetectorEngine();

    private final HighTransactionAmountService highTransactionAmountService;
    private final MultipleServiceTransaction multipleServiceTransaction;
    private final PingPongActivityService pingPongActivityService;
    @Override
    public List<Alert> checkAllFraudActivities(List<TransactionEvent> events) {
        Set<String> distinctUserIDs = new HashSet<>();
        List<Alert> alerts = fraudDetectorEngine.getAlerts();
        for (TransactionEvent event : events) {
            distinctUserIDs.add(event.getUserID());
        }
        for (String userID : distinctUserIDs) {
            List<TransactionEvent> transactions = fraudDetectorEngine.getUserTransactions()
                    .computeIfAbsent(userID, k -> Collections.synchronizedList(new ArrayList<>()));
            List<TransactionEvent> userEvents = events.stream()
                    .filter(event -> event.getUserID().equals(userID))
                    .toList();

            transactions.addAll(userEvents);

            highTransactionAmountService.checkHighAmountTransactions(userEvents, alerts, userID);
            pingPongActivityService.checkPingPongActivity(userEvents, alerts, userID);
            multipleServiceTransaction.checkMultipleServiceTransactions(userEvents, alerts, userID);
        }

        return new ArrayList<>(alerts);
    }

    @Override
    public List<Alert> checkHighTransactionFraud(List<TransactionEvent> events) {
        Set<String> distinctUserIDs = new HashSet<>();
        List<Alert> alerts = fraudDetectorEngine.getAlerts();
        for (TransactionEvent event : events) {
            distinctUserIDs.add(event.getUserID());
        }
        for (String userID : distinctUserIDs) {
            List<TransactionEvent> transactions = fraudDetectorEngine.getUserTransactions()
                    .computeIfAbsent(userID, k -> Collections.synchronizedList(new ArrayList<>()));
            List<TransactionEvent> userEvents = events.stream()
                    .filter(event -> event.getUserID().equals(userID))
                    .toList();

            transactions.addAll(userEvents);

            alerts.addAll(highTransactionAmountService.checkHighAmountTransactions(userEvents, alerts, userID));
        }

        return new ArrayList<>(alerts);
    }

    @Override
    public List<Alert> checkPingPongFraud(List<TransactionEvent> events) {
        Set<String> distinctUserIDs = new HashSet<>();
        List<Alert> alerts = fraudDetectorEngine.getAlerts();
        for (TransactionEvent event : events) {
            distinctUserIDs.add(event.getUserID());
        }
        for (String userID : distinctUserIDs) {
            List<TransactionEvent> transactions = fraudDetectorEngine.getUserTransactions()
                    .computeIfAbsent(userID, k -> Collections.synchronizedList(new ArrayList<>()));
            List<TransactionEvent> userEvents = events.stream()
                    .filter(event -> event.getUserID().equals(userID))
                    .toList();

            transactions.addAll(userEvents);

            alerts.addAll(pingPongActivityService.checkPingPongActivity(userEvents, alerts, userID));
        }

        return new ArrayList<>(alerts);
    }

    @Override
    public List<Alert> checkMultipleServiceFraud(List<TransactionEvent> events) {
        Set<String> distinctUserIDs = new HashSet<>();
        List<Alert> alerts = fraudDetectorEngine.getAlerts();
        for (TransactionEvent event : events) {
            distinctUserIDs.add(event.getUserID());
        }
        for (String userID : distinctUserIDs) {
            List<TransactionEvent> transactions = fraudDetectorEngine.getUserTransactions()
                    .computeIfAbsent(userID, k -> Collections.synchronizedList(new ArrayList<>()));
            List<TransactionEvent> userEvents = events.stream()
                    .filter(event -> event.getUserID().equals(userID))
                    .toList();

            transactions.addAll(userEvents);

            alerts.addAll(multipleServiceTransaction.checkMultipleServiceTransactions(userEvents, alerts, userID));
        }

        return new ArrayList<>(alerts);
    }

}
