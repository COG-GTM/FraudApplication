package com.example.fraudapplication.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
@AllArgsConstructor
@Data
public class FraudDetectorEngine {
    private final Map<String, List<TransactionEvent>> userTransactions = new ConcurrentHashMap<>();
    private final List<Alert> alerts = Collections.synchronizedList(new ArrayList<>());
}
