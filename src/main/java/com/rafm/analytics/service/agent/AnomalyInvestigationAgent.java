package com.rafm.analytics.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafm.analytics.model.Anomaly;
import com.rafm.analytics.model.InvestigationReport;
import com.rafm.analytics.repository.AnomalyRepository;
import com.rafm.analytics.repository.InvestigationReportRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class AnomalyInvestigationAgent {

    private final AnomalyRepository anomalyRepository;
    private final InvestigationReportRepository investigationReportRepository;
    private final AgentTools agentTools;
    private final ObjectMapper objectMapper;

    public AnomalyInvestigationAgent(
            AnomalyRepository anomalyRepository,
            InvestigationReportRepository investigationReportRepository,
            AgentTools agentTools,
            ObjectMapper objectMapper
    ) {
        this.anomalyRepository = anomalyRepository;
        this.investigationReportRepository = investigationReportRepository;
        this.agentTools = agentTools;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> investigate(Long anomalyId) {
        Anomaly anomaly = anomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new NoSuchElementException("Anomaly not found: " + anomalyId));

        Map<String, Object> evidence = agentTools.gatherEvidence(anomaly);
        String classification = agentTools.classify(anomaly);
        double riskScore = agentTools.scoreRisk(anomaly);
        String recommendedAction = agentTools.recommendAction(anomaly, riskScore);

        String reportText = buildReport(anomaly, classification, riskScore, recommendedAction);

        String evidenceJson;
        try {
            evidenceJson = objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException e) {
            evidenceJson = "{\"error\":\"Could not serialize evidence\"}";
        }

        InvestigationReport report = new InvestigationReport();
        report.setAnomalyId(anomaly.getId());
        report.setClassification(classification);
        report.setEvidenceJson(evidenceJson);
        report.setRecommendedAction(recommendedAction);
        report.setConfidenceScore(riskScore);
        report.setReportText(reportText);
        report.setGeneratedAt(Instant.now());

        investigationReportRepository.save(report);

        anomaly.setStatus("INVESTIGATING");
        anomalyRepository.save(anomaly);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("anomalyId", anomaly.getId());
        response.put("anomalyType", anomaly.getType());
        response.put("severity", anomaly.getSeverity());
        response.put("classification", classification);
        response.put("explanation", anomaly.getDescription());
        response.put("evidence", evidence);
        response.put("recommendedAction", recommendedAction);
        response.put("confidenceScore", riskScore);
        response.put("generatedReport", reportText);

        return response;
    }

    private String buildReport(
            Anomaly anomaly,
            String classification,
            double riskScore,
            String recommendedAction
    ) {
        return "RAFM Investigation Report\n"
                + "=========================\n"
                + "Anomaly ID: " + anomaly.getId() + "\n"
                + "Type: " + anomaly.getType() + "\n"
                + "Severity: " + anomaly.getSeverity() + "\n"
                + "Description: " + anomaly.getDescription() + "\n\n"
                + "Classification:\n"
                + classification + "\n\n"
                + "Risk Score:\n"
                + riskScore + "\n\n"
                + "Recommended Action:\n"
                + recommendedAction + "\n";
    }
}
