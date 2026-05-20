package com.example.fraudapplication.controller;

import com.example.fraudapplication.domain.model.Alert;
import com.example.fraudapplication.service.FraudDetectorEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FraudDetectionController.class)
class FraudDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudDetectorEngineService fraudDetectorEngineService;

    @Test
    void getAllFraud_returnsAlertsAndHttp200() throws Exception {
        Alert alert = Alert.builder()
                .userId("user1")
                .alertName("High Transaction Alert")
                .alertMessage("test message")
                .alertTime("2024-03-15 2:30:00 PM")
                .build();

        when(fraudDetectorEngineService.checkAllFraudActivities(anyList()))
                .thenReturn(List.of(alert));

        mockMvc.perform(get("/fraud/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is("user1")))
                .andExpect(jsonPath("$[0].alertName", is("High Transaction Alert")));
    }

    @Test
    void getAllFraud_emptyResult_returnsEmptyJsonArray() throws Exception {
        when(fraudDetectorEngineService.checkAllFraudActivities(anyList()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/fraud/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void downloadCsv_returnsHttp200WithCsvContent() throws Exception {
        mockMvc.perform(get("/fraud/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentType("text/csv"));
    }
}
