package com.rafm.analytics.controller;

import com.rafm.analytics.model.PaymentRecord;
import com.rafm.analytics.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public PaymentRecord createPaymentRecord(@RequestBody PaymentRecord paymentRecord) {
        if (paymentRecord.getPaidAt() == null) {
            paymentRecord.setPaidAt(Instant.now());
        }
        if (paymentRecord.getStatus() == null) {
            paymentRecord.setStatus("SUCCESS");
        }
        if (paymentRecord.getMethod() == null) {
            paymentRecord.setMethod("CARD");
        }
        return paymentRepository.save(paymentRecord);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentRecord> getPaymentRecord(@PathVariable Long id) {
        return paymentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PaymentRecord> getPaymentRecords(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long billingId
    ) {
        if (billingId != null) {
            return paymentRepository.findByBillingId(billingId);
        }
        if (customerId != null) {
            return paymentRepository.findByCustomerId(customerId);
        }
        return paymentRepository.findAll();
    }
}
