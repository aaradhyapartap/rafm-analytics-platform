package com.rafm.analytics.controller;

import com.rafm.analytics.model.Anomaly;
import com.rafm.analytics.repository.AnomalyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final AnomalyRepository anomalyRepository;

    public AnomalyController(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    @GetMapping
    public List<Anomaly> getAnomalies(@RequestParam(required = false) String status) {
        if (status == null) {
            return anomalyRepository.findAll();
        }
        return anomalyRepository.findByStatus(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Anomaly> getAnomaly(@PathVariable Long id) {
        return anomalyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
