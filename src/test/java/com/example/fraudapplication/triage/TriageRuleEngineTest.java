package com.example.fraudapplication.triage;

import com.example.fraudapplication.triage.model.AlertContext;
import com.example.fraudapplication.triage.model.TriageAlert;
import com.example.fraudapplication.triage.model.TriageDisposition;
import com.example.fraudapplication.triage.model.TriageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriageRuleEngineTest {

    private final TriageRuleEngine engine = new TriageRuleEngine();

    private TriageAlert.TriageAlertBuilder baseAlert() {
        return TriageAlert.builder()
                .alertId("T-1")
                .alertType("HIGH_TRANSACTION")
                .customerRef("CUST-1")
                .amount(1000.0)
                .currency("GBP")
                .riskScore(30);
    }

    private AlertContext.AlertContextBuilder baseContext() {
        return AlertContext.builder()
                .kycVerified(true)
                .sanctionsHit(false)
                .pepMatch(false)
                .whitelistedCounterparty(false)
                .counterpartyType("RECURRING_PAYEE")
                .accountType("PERSONAL")
                .structuringPattern(false)
                .geoConsistent(true)
                .priorSarCount(0);
    }

    @Test
    void sanctionsHitEscalatesRegardlessOfType() {
        TriageAlert alert = baseAlert()
                .context(baseContext().sanctionsHit(true).build())
                .build();

        TriageResult result = engine.evaluate(alert);

        assertEquals(TriageDisposition.ESCALATE_SAR, result.getDisposition());
        assertEquals(1, result.getRuleFired());
        assertTrue(result.isRequiresHumanReview());
    }

    @Test
    void pepMatchEscalates() {
        TriageAlert alert = baseAlert()
                .alertType("PING_PONG")
                .context(baseContext().counterpartyType("EXTERNAL_ACCOUNT").pepMatch(true).build())
                .build();

        assertEquals(TriageDisposition.ESCALATE_SAR, engine.evaluate(alert).getDisposition());
    }

    @Test
    void structuringPatternEscalates() {
        TriageAlert alert = baseAlert()
                .context(baseContext().counterpartyType("NEW_PAYEE").structuringPattern(true).build())
                .build();

        assertEquals(TriageDisposition.ESCALATE_SAR, engine.evaluate(alert).getDisposition());
    }

    @Test
    void priorSarEscalates() {
        TriageAlert alert = baseAlert()
                .context(baseContext().counterpartyType("NEW_PAYEE").priorSarCount(2).build())
                .build();

        assertEquals(TriageDisposition.ESCALATE_SAR, engine.evaluate(alert).getDisposition());
    }

    @Test
    void highRiskScoreEscalatesWhenNoRedFlags() {
        TriageAlert alert = baseAlert()
                .riskScore(85)
                .context(baseContext().counterpartyType("NEW_PAYEE").build())
                .build();

        TriageResult result = engine.evaluate(alert);
        assertEquals(TriageDisposition.ESCALATE_SAR, result.getDisposition());
        assertEquals(2, result.getRuleFired());
    }

    @Test
    void recurringPayeeHighTransactionAutoCloses() {
        TriageAlert alert = baseAlert()
                .riskScore(35)
                .context(baseContext().counterpartyType("RECURRING_PAYEE").build())
                .build();

        TriageResult result = engine.evaluate(alert);
        assertEquals(TriageDisposition.AUTO_CLOSE_FALSE_POSITIVE, result.getDisposition());
        assertEquals(3, result.getRuleFired());
        assertFalse(result.isRequiresHumanReview());
    }

    @Test
    void internalPingPongAutoCloses() {
        TriageAlert alert = baseAlert()
                .alertType("PING_PONG")
                .riskScore(25)
                .context(baseContext().counterpartyType("INTERNAL_ACCOUNT").build())
                .build();

        assertEquals(TriageDisposition.AUTO_CLOSE_FALSE_POSITIVE, engine.evaluate(alert).getDisposition());
    }

    @Test
    void businessMultipleServiceAutoCloses() {
        TriageAlert alert = baseAlert()
                .alertType("MULTIPLE_SERVICE")
                .riskScore(45)
                .context(baseContext().counterpartyType("MERCHANT").accountType("BUSINESS").build())
                .build();

        assertEquals(TriageDisposition.AUTO_CLOSE_FALSE_POSITIVE, engine.evaluate(alert).getDisposition());
    }

    @Test
    void autoCloseCeilingSendsBorderlineToInvestigate() {
        // Same known-good pattern but risk score at/above the auto-close ceiling.
        TriageAlert alert = baseAlert()
                .riskScore(65)
                .context(baseContext().counterpartyType("RECURRING_PAYEE").build())
                .build();

        TriageResult result = engine.evaluate(alert);
        assertEquals(TriageDisposition.INVESTIGATE, result.getDisposition());
        assertEquals(4, result.getRuleFired());
    }

    @Test
    void newPayeeWithoutRedFlagsInvestigates() {
        TriageAlert alert = baseAlert()
                .riskScore(68)
                .context(baseContext().counterpartyType("NEW_PAYEE").build())
                .build();

        assertEquals(TriageDisposition.INVESTIGATE, engine.evaluate(alert).getDisposition());
    }
}
