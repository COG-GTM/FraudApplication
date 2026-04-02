package com.example.fraudapplication.controller;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.service.FraudDetectorEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@WebMvcTest(FraudDetectionController.class)
class FraudDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudDetectorEngineService fraudDetectorEngineService;

    @Test
    void testCheckAllFraudTransactions() throws Exception {
        List<Alert> alerts = new ArrayList<>();
        alerts.add(Alert.builder().userId("user1").alertName("High Transaction Alert")
                .alertMessage("Transaction amount is 5x above the user's average in the last 24 hours")
                .alertTime("2024-03-15 2:30:00 PM").build());

        when(fraudDetectorEngineService.checkAllFraudActivities(anyList())).thenReturn(alerts);

        mockMvc.perform(get("/fraud/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user1"))
                .andExpect(jsonPath("$[0].alertName").value("High Transaction Alert"));
    }

    @Test
    void testCheckAllFraudTransactions_empty() throws Exception {
        when(fraudDetectorEngineService.checkAllFraudActivities(anyList())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/fraud/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testExportCsv() throws Exception {
        mockMvc.perform(get("/fraud/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"fraudDetection.csv\""))
                .andExpect(content().contentType("text/csv"));
    }
}
