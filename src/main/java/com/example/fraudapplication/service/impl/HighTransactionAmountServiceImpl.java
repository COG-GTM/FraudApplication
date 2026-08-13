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

    /** FR-HTA-001: rolling baseline window, in seconds. */
    public static final long BASELINE_WINDOW_SECONDS = 24 * 60 * 60;

    /** FR-HTA-001: a transaction is flagged at or above this multiple of the baseline. */
    public static final double HIGH_AMOUNT_MULTIPLIER = 5.0;

    private final AlertGenerator alertGenerator;

    @Override
    public List<Alert> checkHighAmountTransactions(List<TransactionEvent> transactions, List<Alert> alerts,
                                                   String userId) {
        if (transactions == null || transactions.isEmpty()) {
            return alerts;
        }

        Instant windowStart = Instant.now().minusSeconds(BASELINE_WINDOW_SECONDS);
        List<TransactionEvent> scored = transactions.stream()
                .filter(event -> event != null && event.getTimestamp() != null)
                .filter(event -> !event.getTimestamp().isBefore(windowStart))
                .filter(event -> isValidAmount(event.getAmount()))
                .toList();

        if (scored.size() < 2) {
            return alerts;
        }

        double totalAmount = scored.stream().mapToDouble(TransactionEvent::getAmount).sum();

        for (TransactionEvent event : scored) {
            double baseline = (totalAmount - event.getAmount()) / (scored.size() - 1);
            if (baseline > 0 && event.getAmount() >= (HIGH_AMOUNT_MULTIPLIER * baseline)) {
                alerts.add(alertGenerator.generateHighTransactionAlert(userId));
            }
        }
        return alerts;
    }

    private static boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }
}
