package com.rafm.analytics.controller;

import com.rafm.analytics.model.Anomaly;
import com.rafm.analytics.service.AnomalyDetectionService;
import com.rafm.analytics.service.SyntheticDataService;
import com.rafm.analytics.service.TelcoCsvIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SyntheticDataService synth;
    private final AnomalyDetectionService detector;
    private final TelcoCsvIngestionService telcoIngest;

    @PostMapping("/seed")
    public Map<String, Integer> seed(@RequestParam(defaultValue = "50") int customers) {
        return synth.seed(customers);
    }

    @PostMapping("/detect")
    public List<Anomaly> detectNow() {
        return detector.runAllRules();
    }

    @PostMapping("/ingest-telco")
    public Map<String, Integer> ingestTelco(
            @RequestParam(defaultValue = "data/raw/telco-churn.csv") String path,
            @RequestParam(defaultValue = "0") int maxRows) throws Exception {
        return telcoIngest.ingest(path, maxRows);
    }
}
