package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.HighTransactionAmountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HighTransactionAmountServiceImpl implements HighTransactionAmountService {

    private final AlertGenerator alertGenerator;

    @Override
    public List<Alert> checkHighAmountTransactions(List<TransactionEvent> transactions, List<Alert> alerts,
                                                   String userId) {
        double totalAmount = transactions.stream()
                .filter(event -> event.getTimestamp().isAfter(Instant.now().minusSeconds(24 * 60 * 60)))
                .mapToDouble(TransactionEvent::getAmount)
                .sum();

        long total24HoursTransaction = transactions.stream()
                .filter(event -> event.getTimestamp().isAfter(Instant.now().minusSeconds(24 * 60 * 60)))
                .toList().size();

        if (total24HoursTransaction == 0) return alerts;

        if (!transactions.isEmpty()) {
            double averageAmount = totalAmount / total24HoursTransaction;
            for(TransactionEvent event : transactions){
                if (event.getAmount() > (5 * averageAmount)) {
                    alerts.add(alertGenerator.generateHighTransactionAlert(userId));
                }
            }

        }
        return alerts;
    }
}
