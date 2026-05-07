package com.rafm.analytics.service;

import com.rafm.analytics.model.Anomaly;
import com.rafm.analytics.model.BillingRecord;
import com.rafm.analytics.model.PaymentRecord;
import com.rafm.analytics.repository.AnomalyRepository;
import com.rafm.analytics.repository.BillingRepository;
import com.rafm.analytics.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyDetectionService {

    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final AnomalyRepository anomalyRepository;

    public AnomalyDetectionService(
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            AnomalyRepository anomalyRepository
    ) {
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.anomalyRepository = anomalyRepository;
    }

    public List<Anomaly> runDetection() {
        List<Anomaly> anomalies = new ArrayList<>();

        for (BillingRecord billing : billingRepository.findAll()) {
            List<PaymentRecord> payments = paymentRepository.findByBillingId(billing.getId());

            BigDecimal totalPaid = payments.stream()
                    .filter(payment -> "SUCCESS".equalsIgnoreCase(payment.getStatus()))
                    .map(PaymentRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (payments.size() > 1) {
                anomalies.add(createAnomaly(
                        "DUPLICATE_PAYMENT",
                        "HIGH",
                        billing.getCustomerId(),
                        "BILLING",
                        billing.getId(),
                        "Multiple payments found for billing record " + billing.getId()
                ));
            }

            if (totalPaid.compareTo(BigDecimal.ZERO) > 0 && totalPaid.compareTo(billing.getAmount()) != 0) {
                anomalies.add(createAnomaly(
                        "BILLING_PAYMENT_MISMATCH",
                        "MEDIUM",
                        billing.getCustomerId(),
                        "BILLING",
                        billing.getId(),
                        "Billing amount is " + billing.getAmount() + " but successful payment total is " + totalPaid
                ));
            }
        }

        return anomalyRepository.saveAll(anomalies);
    }

    private Anomaly createAnomaly(
            String type,
            String severity,
            Long customerId,
            String relatedEntity,
            Long relatedId,
            String description
    ) {
        Anomaly anomaly = new Anomaly();
        anomaly.setType(type);
        anomaly.setSeverity(severity);
        anomaly.setCustomerId(customerId);
        anomaly.setRelatedEntity(relatedEntity);
        anomaly.setRelatedId(relatedId);
        anomaly.setDescription(description);
        anomaly.setStatus("OPEN");
        anomaly.setDetectedAt(Instant.now());
        return anomaly;
    }
}
