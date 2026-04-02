package com.example.fraudapplication.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TransactionEventTest {

    @Test
    void testBuilder() {
        Instant now = Instant.now();
        TransactionEvent event = TransactionEvent.builder()
                .timestamp(now)
                .amount(150.0)
                .userID("user1")
                .serviceID("serviceA")
                .build();

        assertEquals(now, event.getTimestamp());
        assertEquals(150.0, event.getAmount());
        assertEquals("user1", event.getUserID());
        assertEquals("serviceA", event.getServiceID());
    }

    @Test
    void testSetters() {
        Instant now = Instant.now();
        TransactionEvent event = new TransactionEvent();
        event.setTimestamp(now);
        event.setAmount(200.0);
        event.setUserID("user2");
        event.setServiceID("serviceB");

        assertEquals(now, event.getTimestamp());
        assertEquals(200.0, event.getAmount());
        assertEquals("user2", event.getUserID());
        assertEquals("serviceB", event.getServiceID());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.now();
        TransactionEvent event1 = TransactionEvent.builder().timestamp(now).amount(100.0).userID("user1").serviceID("sA").build();
        TransactionEvent event2 = TransactionEvent.builder().timestamp(now).amount(100.0).userID("user1").serviceID("sA").build();
        TransactionEvent event3 = TransactionEvent.builder().timestamp(now).amount(200.0).userID("user2").serviceID("sB").build();

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
        assertNotEquals(event1, event3);
    }

    @Test
    void testToString() {
        TransactionEvent event = TransactionEvent.builder().timestamp(Instant.now()).amount(100.0).userID("user1").serviceID("sA").build();
        assertNotNull(event.toString());
    }

    @Test
    void testNoArgsConstructor() {
        TransactionEvent event = new TransactionEvent();
        assertNotNull(event);
        assertNull(event.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        Instant now = Instant.now();
        TransactionEvent event = new TransactionEvent(now, 100.0, "user1", "sA");
        assertEquals(now, event.getTimestamp());
        assertEquals(100.0, event.getAmount());
        assertEquals("user1", event.getUserID());
        assertEquals("sA", event.getServiceID());
    }
}
