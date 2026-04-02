package com.example.fraudapplication.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {

    @Test
    void testBuilder() {
        Alert alert = Alert.builder()
                .userId("user1")
                .alertName("High Transaction Alert")
                .alertMessage("msg")
                .alertTime("2024-01-01 12:00:00 PM")
                .build();

        assertEquals("user1", alert.getUserId());
        assertEquals("High Transaction Alert", alert.getAlertName());
        assertEquals("msg", alert.getAlertMessage());
        assertEquals("2024-01-01 12:00:00 PM", alert.getAlertTime());
    }

    @Test
    void testSetters() {
        Alert alert = new Alert();
        alert.setUserId("user2");
        alert.setAlertName("Multiple Service Alert");
        alert.setAlertMessage("message");
        alert.setAlertTime("time");

        assertEquals("user2", alert.getUserId());
        assertEquals("Multiple Service Alert", alert.getAlertName());
        assertEquals("message", alert.getAlertMessage());
        assertEquals("time", alert.getAlertTime());
    }

    @Test
    void testEqualsAndHashCode() {
        Alert alert1 = Alert.builder().userId("user1").alertName("name").alertMessage("msg").alertTime("time").build();
        Alert alert2 = Alert.builder().userId("user1").alertName("name").alertMessage("msg").alertTime("time").build();
        Alert alert3 = Alert.builder().userId("user2").alertName("name").alertMessage("msg").alertTime("time").build();

        assertEquals(alert1, alert2);
        assertEquals(alert1.hashCode(), alert2.hashCode());
        assertNotEquals(alert1, alert3);
    }

    @Test
    void testToString() {
        Alert alert = Alert.builder().userId("user1").alertName("name").alertMessage("msg").alertTime("time").build();
        assertNotNull(alert.toString());
    }

    @Test
    void testNoArgsConstructor() {
        Alert alert = new Alert();
        assertNotNull(alert);
        assertNull(alert.getUserId());
    }

    @Test
    void testAllArgsConstructor() {
        Alert alert = new Alert("user1", "name", "msg", "time");
        assertEquals("user1", alert.getUserId());
        assertEquals("name", alert.getAlertName());
    }
}
