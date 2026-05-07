package com.rafm.analytics.controller;

import com.rafm.analytics.model.Anomaly;
import com.rafm.analytics.service.AnomalyDetectionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AnomalyDetectionService anomalyDetectionService;

    public AdminController(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @PostMapping("/detect")
    public List<Anomaly> detectAnomalies() {
        return anomalyDetectionService.runDetection();
    }
}
