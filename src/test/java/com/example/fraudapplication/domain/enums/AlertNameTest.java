package com.example.fraudapplication.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertNameTest {

    @Test
    void highTransaction_getAlertName() {
        assertEquals("High Transaction Alert", AlertName.HIGH_TRANSACTION.getAlertName());
    }

    @Test
    void multipleService_getAlertMessage() {
        assertEquals("User conducting transaction in more than 3 distinct services within 5-minute window ",
                AlertName.MULTIPLE_SERVICE.getAlertMessage());
    }

    @Test
    void pingPong_getAlertNameAndMessage() {
        assertEquals("Ping-Pong Activity Alert", AlertName.PING_PONG.getAlertName());
        assertEquals("User's transactions bouncing back and forth between two services within 10-minute window",
                AlertName.PING_PONG.getAlertMessage());
    }

    @Test
    void values_returnsAll3Constants() {
        AlertName[] values = AlertName.values();
        assertEquals(3, values.length);
    }

    @Test
    void valueOf_highTransaction() {
        AlertName result = AlertName.valueOf("HIGH_TRANSACTION");
        assertEquals(AlertName.HIGH_TRANSACTION, result);
    }
}
