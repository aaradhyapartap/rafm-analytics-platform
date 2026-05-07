package com.rafm.analytics.controller;

import com.rafm.analytics.model.UsageRecord;
import com.rafm.analytics.repository.UsageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageRepository usageRepository;

    public UsageController(UsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    @PostMapping
    public UsageRecord createUsageRecord(@RequestBody UsageRecord usageRecord) {
        if (usageRecord.getOccurredAt() == null) {
            usageRecord.setOccurredAt(Instant.now());
        }
        if (usageRecord.getUsageType() == null) {
            usageRecord.setUsageType("DATA");
        }
        return usageRepository.save(usageRecord);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsageRecord> getUsageRecord(@PathVariable Long id) {
        return usageRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<UsageRecord> getUsageRecords(@RequestParam(required = false) Long customerId) {
        if (customerId == null) {
            return usageRepository.findAll();
        }
        return usageRepository.findByCustomerId(customerId);
    }
}
