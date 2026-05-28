package com.rafm.analytics.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafm.analytics.model.*;
import com.rafm.analytics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Deterministic, auditable investigation workflow. Every step is logged into
 * the report's evidence trail so the output is reproducible and explainable —
 * the same shape we'd swap in a tool-calling LLM under, later.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyInvestigationAgent {

    private final AnomalyRepository anomalyRepo;
    private final InvestigationReportRepository reportRepo;
    private final AgentTools tools;
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> investigate(Long anomalyId) {
        Anomaly anomaly = anomalyRepo.findById(anomalyId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Anomaly " + anomalyId + " not found"));

        List<String> trace = new ArrayList<>();
        trace.add("step=receive id=" + anomalyId + " type=" + anomaly.getType());

        Customer customer = tools.customerLookup(anomaly.getCustomerId()).orElse(null);
        trace.add("step=customer_lookup found=" + (customer != null));

        List<BillingRecord> bills = tools.billingLookup(anomaly.getCustomerId());
        List<PaymentRecord> pays = tools.paymentLookup(anomaly.getCustomerId());
        List<UsageRecord> usage = tools.usageLookup(anomaly.getCustomerId());
        trace.add("step=context_gather bills=" + bills.size()
                + " pays=" + pays.size() + " usage=" + usage.size());

        String classification = tools.classify(anomaly);
        trace.add("step=classify result=" + classification);

        double risk = tools.riskScore(anomaly, customer);
        trace.add("step=risk_score value=" + String.format("%.2f", risk));

        String action = tools.recommend(anomaly, risk);
        trace.add("step=recommend action=" + action);

        String narrative = buildReport(anomaly, customer, bills.size(), pays.size(),
                usage.size(), classification, risk, action);
        trace.add("step=report_generated len=" + narrative.length());

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("anomalyId", anomaly.getId());
        evidence.put("anomalyType", anomaly.getType());
        evidence.put("severity", anomaly.getSeverity());
        evidence.put("customerId", anomaly.getCustomerId());
        evidence.put("customerRiskTier",
                customer == null ? null : customer.getRiskTier());
        evidence.put("billingRecordCount", bills.size());
        evidence.put("paymentRecordCount", pays.size());
        evidence.put("usageRecordCount", usage.size());
        evidence.put("trace", trace);

        String evidenceJson;
        try {
            evidenceJson = mapper.writeValueAsString(evidence);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize evidence: {}", e.getMessage());
            evidenceJson = "{\"error\":\"serialization\"}";
        }

        InvestigationReport report = InvestigationReport.builder()
                .anomalyId(anomaly.getId())
                .classification(classification)
                .evidenceJson(evidenceJson)
                .recommendedAction(action)
                .confidenceScore(risk)
                .reportText(narrative)
                .generatedAt(Instant.now())
                .build();
        reportRepo.save(report);

        anomaly.setStatus("INVESTIGATING");
        anomalyRepo.save(anomaly);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("anomalyId", anomaly.getId());
        result.put("anomalyType", anomaly.getType());
        result.put("severity", anomaly.getSeverity());
        result.put("classification", classification);
        result.put("explanation", anomaly.getDescription());
        result.put("evidence", evidence);
        result.put("recommendedAction", action);
        result.put("confidenceScore", risk);
        result.put("generatedReport", narrative);
        return result;
    }

    private String buildReport(Anomaly a, Customer c, int bills, int pays, int usage,
                               String cls, double risk, String action) {
        return """
                RAFM Investigation Report
                =========================
                Anomaly ID    : %d
                Type          : %s   Severity: %s
                Customer      : %s (id=%s, tier=%s)
                Description   : %s

                Context gathered
                ----------------
                Billing records reviewed: %d
                Payment records reviewed: %d
                Usage records reviewed  : %d

                Classification
                --------------
                %s

                Risk assessment
                ---------------
                Confidence score: %.2f / 1.00

                Recommended action
                ------------------
                %s
                """.formatted(
                a.getId(), a.getType(), a.getSeverity(),
                c == null ? "UNKNOWN" : c.getName(),
                c == null ? "?" : c.getId().toString(),
                c == null ? "?" : c.getRiskTier(),
                a.getDescription(),
                bills, pays, usage,
                cls, risk, action);
    }
}
