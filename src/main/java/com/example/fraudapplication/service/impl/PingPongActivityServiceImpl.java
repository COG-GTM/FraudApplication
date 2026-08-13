package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.PingPongActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PingPongActivityServiceImpl implements PingPongActivityService {

    /** FR-PPA-003: window covering the bounce sequence, in seconds (boundary inclusive). */
    public static final long PING_PONG_WINDOW_SECONDS = 10 * 60;

    private static final Duration PING_PONG_WINDOW = Duration.ofSeconds(PING_PONG_WINDOW_SECONDS);

    /** FR-PPA-003: number of consecutive alternating transactions that constitute a bounce. */
    public static final int PING_PONG_SEQUENCE_LENGTH = 4;

    private final AlertGenerator alertGenerator;

    @Override
    public List<Alert> checkPingPongActivity(List<TransactionEvent> transactions, List<Alert> alerts,
                                             String userId) {
        if (transactions == null || transactions.size() < PING_PONG_SEQUENCE_LENGTH) {
            return alerts;
        }

        List<TransactionEvent> ordered = transactions.stream()
                .filter(event -> event != null && event.getTimestamp() != null && event.getServiceID() != null)
                .sorted(Comparator.comparing(TransactionEvent::getTimestamp))
                .toList();

        int i = PING_PONG_SEQUENCE_LENGTH - 1;
        while (i < ordered.size()) {
            TransactionEvent first = ordered.get(i - 3);
            TransactionEvent second = ordered.get(i - 2);
            TransactionEvent third = ordered.get(i - 1);
            TransactionEvent fourth = ordered.get(i);

            boolean alternating = !first.getServiceID().equals(second.getServiceID())
                    && first.getServiceID().equals(third.getServiceID())
                    && second.getServiceID().equals(fourth.getServiceID());
            boolean withinWindow = Duration.between(first.getTimestamp(), fourth.getTimestamp())
                    .compareTo(PING_PONG_WINDOW) <= 0;

            if (alternating && withinWindow) {
                alerts.add(alertGenerator.generatePingPongAlert(userId));
                i += PING_PONG_SEQUENCE_LENGTH;
            } else {
                i++;
            }
        }
        return alerts;
    }

}
