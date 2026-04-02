package com.example.fraudapplication.service.impl;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.domain.model.TransactionEvent;
import com.example.fraudapplication.service.AlertGenerator;
import com.example.fraudapplication.service.PingPongActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PingPongActivityServiceImpl implements PingPongActivityService {

    private final AlertGenerator alertGenerator;

    @Override
    public List<Alert> checkPingPongActivity(List<TransactionEvent> transactions, List<Alert> alerts,
                                             String userId) {

        if (transactions == null || transactions.isEmpty()) return alerts;

        Map<String, TransactionEvent> last2ServiceMap = new LinkedHashMap<>();
        TransactionEvent firstTransaction = transactions.get(0);

        last2ServiceMap.put(firstTransaction.getServiceID(), firstTransaction);

        int index = 0;
        for (int i = 1; i < transactions.size(); i++) {
            TransactionEvent nextTransaction = transactions.get(i);
            if (!firstTransaction.getServiceID().equals(nextTransaction.getServiceID())
                    && Duration.between(firstTransaction.getTimestamp(),
                    nextTransaction.getTimestamp()).getSeconds() <= (10 * 60)) {
                last2ServiceMap.put(nextTransaction.getServiceID(), nextTransaction);
            }
            if (last2ServiceMap.size() == 2) {
                index = transactions.lastIndexOf(nextTransaction);
                break;
            }
        }

        if (last2ServiceMap.size() < 2) return alerts;

        for (int j = index + 2; j < transactions.size() - 1; j++) {
            TransactionEvent currentTransaction = transactions.get(j);
            TransactionEvent previousTransaction = transactions.get(j - 1);

            Iterator<Map.Entry<String, TransactionEvent>> iterator = last2ServiceMap.entrySet().iterator();
            Map.Entry<String, TransactionEvent> firstEntry = iterator.next();
            Map.Entry<String, TransactionEvent> secondEntry = iterator.next();

            firstTransaction = firstEntry.getValue();
            TransactionEvent secondTransaction = secondEntry.getValue();

            Duration duration = Duration.between(firstTransaction.getTimestamp(), currentTransaction.getTimestamp());

            if (firstTransaction.getServiceID().equals(previousTransaction.getServiceID()) &&
                    secondTransaction.getServiceID().equals(currentTransaction.getServiceID()) &&
                    duration.getSeconds() <= (10 * 60)) {
                alerts.add(alertGenerator.generatePingPongAlert(userId));
                last2ServiceMap.clear();
                last2ServiceMap.put(previousTransaction.getServiceID(), previousTransaction);
                last2ServiceMap.put(currentTransaction.getServiceID(), currentTransaction);
            } else if (!secondTransaction.getServiceID().equals(previousTransaction.getServiceID())) {
                String removedService = firstTransaction.getServiceID();
                last2ServiceMap.remove(removedService);
                last2ServiceMap.put(previousTransaction.getServiceID(), previousTransaction);
            }
        }
        return alerts;
    }

}
