package com.example.fraudapplication.domain.utils;

import com.example.fraudapplication.domain.model.TransactionEvent;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListToCsvTest {

    private static final String TEST_OUTPUT = "test-output";

    @AfterEach
    void cleanup() {
        File file = new File(TEST_OUTPUT + ".csv");
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testExportCSVToFile() {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(TransactionEvent.builder().timestamp(Instant.now()).amount(100.0).userID("user1").serviceID("sA").build());
        events.add(TransactionEvent.builder().timestamp(Instant.now()).amount(200.0).userID("user2").serviceID("sB").build());

        ListToCsv.exportCSV(events, TEST_OUTPUT);

        File file = new File(TEST_OUTPUT + ".csv");
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void testExportCSVToFile_emptyList() {
        ListToCsv.exportCSV(new ArrayList<>(), TEST_OUTPUT);

        File file = new File(TEST_OUTPUT + ".csv");
        assertFalse(file.exists());
    }

    @Test
    void testExportCSVToFile_nullList() {
        ListToCsv.exportCSV(null, TEST_OUTPUT);

        File file = new File(TEST_OUTPUT + ".csv");
        assertFalse(file.exists());
    }

    @Test
    void testExportCSVToHttpResponse() throws Exception {
        List<TransactionEvent> events = new ArrayList<>();
        events.add(TransactionEvent.builder().timestamp(Instant.now()).amount(100.0).userID("user1").serviceID("sA").build());

        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        ListToCsv.exportCSV(events, response, "test");

        printWriter.flush();
        String csvContent = stringWriter.toString();
        assertFalse(csvContent.isEmpty());
        assertTrue(csvContent.contains("timestamp"));
        assertTrue(csvContent.contains("amount"));
        assertTrue(csvContent.contains("userID"));
        assertTrue(csvContent.contains("serviceID"));

        verify(response).setContentType("text/csv");
        verify(response).setHeader(eq("Content-Disposition"), eq("attachment; filename=\"test.csv\""));
    }

    @Test
    void testExportCSVToHttpResponse_emptyList() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        ListToCsv.exportCSV(new ArrayList<>(), response, "test");

        verify(response, never()).setContentType(anyString());
    }

    @Test
    void testExportCSVToHttpResponse_nullList() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        ListToCsv.exportCSV(null, response, "test");

        verify(response, never()).setContentType(anyString());
    }

    @Test
    void testGenerateData_withNullField() {
        List<TransactionEvent> events = new ArrayList<>();
        TransactionEvent event = new TransactionEvent();
        event.setAmount(100.0);
        // timestamp, userID, serviceID are null
        events.add(event);

        // Should not throw, and should handle nulls gracefully
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        try {
            when(response.getWriter()).thenReturn(printWriter);
        } catch (Exception e) {
            fail("Should not throw exception");
        }

        ListToCsv.exportCSV(events, response, "test-null");
        printWriter.flush();
        String csvContent = stringWriter.toString();
        // Null fields should be empty strings, not "null"
        assertFalse(csvContent.contains("null"));
    }
}
