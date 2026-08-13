package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.MultipleServiceTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MultipleServiceTransactionImpl implements MultipleServiceTransaction {

    /** FR-MST-002: sliding window length, in seconds (boundary inclusive). */
    public static final long DISTINCT_SERVICE_WINDOW_SECONDS = 5 * 60;

    private static final Duration DISTINCT_SERVICE_WINDOW = Duration.ofSeconds(DISTINCT_SERVICE_WINDOW_SECONDS);

    /** FR-MST-002: more than this many distinct services inside the window is fraudulent. */
    public static final int DISTINCT_SERVICE_THRESHOLD = 3;

    private final AlertGenerator alertGenerator;

    @Override
    public List<Alert> checkMultipleServiceTransactions(List<TransactionEvent> transactions, List<Alert> alerts,
                                                        String userId) {
        if (transactions == null || transactions.isEmpty()) {
            return alerts;
        }

        List<TransactionEvent> ordered = transactions.stream()
                .filter(event -> event != null && event.getTimestamp() != null && event.getServiceID() != null)
                .sorted(Comparator.comparing(TransactionEvent::getTimestamp))
                .toList();

        Deque<TransactionEvent> window = new ArrayDeque<>();
        for (TransactionEvent event : ordered) {
            window.addLast(event);
            while (!window.isEmpty() && Duration.between(window.peekFirst().getTimestamp(),
                    event.getTimestamp()).compareTo(DISTINCT_SERVICE_WINDOW) > 0) {
                window.removeFirst();
            }

            Set<String> distinctServices = new HashSet<>();
            for (TransactionEvent windowed : window) {
                distinctServices.add(windowed.getServiceID());
            }

            if (distinctServices.size() > DISTINCT_SERVICE_THRESHOLD) {
                alerts.add(alertGenerator.generateMultipleServiceAlert(userId));
                window.clear();
            }
        }
        return alerts;
    }
}
