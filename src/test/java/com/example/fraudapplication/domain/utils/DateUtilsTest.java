package com.example.fraudapplication.domain.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void testStringifyDate() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30, 0);
        String result = DateUtils.stringifyDate(dateTime);
        assertEquals("2024-03-15 2:30:00 PM", result);
    }

    @Test
    void testStringifyDate_midnight() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        String result = DateUtils.stringifyDate(dateTime);
        assertEquals("2024-01-01 12:00:00 AM", result);
    }

    @Test
    void testStringifyDate_noon() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        String result = DateUtils.stringifyDate(dateTime);
        assertEquals("2024-06-15 12:00:00 PM", result);
    }
}
