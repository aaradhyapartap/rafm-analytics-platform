package com.rafm.analytics.controller;

import com.rafm.analytics.model.InvestigationReport;
import com.rafm.analytics.repository.InvestigationReportRepository;
import com.rafm.analytics.service.agent.AnomalyInvestigationAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private final AnomalyInvestigationAgent anomalyInvestigationAgent;
    private final InvestigationReportRepository investigationReportRepository;

    public InvestigationController(
            AnomalyInvestigationAgent anomalyInvestigationAgent,
            InvestigationReportRepository investigationReportRepository
    ) {
        this.anomalyInvestigationAgent = anomalyInvestigationAgent;
        this.investigationReportRepository = investigationReportRepository;
    }

    @PostMapping("/{anomalyId}")
    public Map<String, Object> investigate(@PathVariable Long anomalyId) {
        return anomalyInvestigationAgent.investigate(anomalyId);
    }

    @GetMapping("/{anomalyId}")
    public ResponseEntity<InvestigationReport> getReport(@PathVariable Long anomalyId) {
        return investigationReportRepository.findByAnomalyId(anomalyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
