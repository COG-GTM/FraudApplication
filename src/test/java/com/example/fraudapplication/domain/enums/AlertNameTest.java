package com.example.fraudapplication.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertNameTest {

    @Test
    void testHighTransactionValues() {
        assertEquals("High Transaction Alert", AlertName.HIGH_TRANSACTION.getAlertName());
        assertEquals("Transaction amount is 5x above the user's average in the last 24 hours",
                AlertName.HIGH_TRANSACTION.getAlertMessage());
    }

    @Test
    void testMultipleServiceValues() {
        assertEquals("Multiple Service Alert", AlertName.MULTIPLE_SERVICE.getAlertName());
        assertEquals("User conducting transaction in more than 3 distinct services within 5-minute window ",
                AlertName.MULTIPLE_SERVICE.getAlertMessage());
    }

    @Test
    void testPingPongValues() {
        assertEquals("Ping-Pong Activity Alert", AlertName.PING_PONG.getAlertName());
        assertEquals("User's transactions bouncing back and forth between two services within 10-minute window",
                AlertName.PING_PONG.getAlertMessage());
    }

    @Test
    void testValuesCount() {
        assertEquals(3, AlertName.values().length);
    }
}
