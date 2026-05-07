package com.rafm.analytics.controller;

import com.rafm.analytics.model.BillingRecord;
import com.rafm.analytics.repository.BillingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingRepository billingRepository;

    public BillingController(BillingRepository billingRepository) {
        this.billingRepository = billingRepository;
    }

    @PostMapping
    public BillingRecord createBillingRecord(@RequestBody BillingRecord billingRecord) {
        if (billingRecord.getBillingDate() == null) {
            billingRecord.setBillingDate(Instant.now());
        }
        if (billingRecord.getCreatedAt() == null) {
            billingRecord.setCreatedAt(Instant.now());
        }
        if (billingRecord.getStatus() == null) {
            billingRecord.setStatus("ISSUED");
        }
        return billingRepository.save(billingRecord);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillingRecord> getBillingRecord(@PathVariable Long id) {
        return billingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<BillingRecord> getBillingRecords(@RequestParam(required = false) Long customerId) {
        if (customerId == null) {
            return billingRepository.findAll();
        }
        return billingRepository.findByCustomerId(customerId);
    }
}
