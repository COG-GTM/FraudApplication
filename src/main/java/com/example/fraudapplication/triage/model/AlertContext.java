package com.example.fraudapplication.triage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enrichment context attached to a fraud alert. These fields are the signals the
 * triage rules reason over to reach a disposition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertContext {

    private boolean kycVerified;
    private boolean sanctionsHit;
    private boolean pepMatch;
    private boolean whitelistedCounterparty;
    private String counterpartyType;
    private double amountVsAvgRatio;
    private int distinctServices;
    private String accountType;
    private boolean structuringPattern;
    private boolean geoConsistent;
    private int priorSarCount;
    private int customerTenureDays;
    private String note;
}
