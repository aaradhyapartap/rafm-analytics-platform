package com.rafm.analytics.service;

import com.rafm.analytics.model.*;
import com.rafm.analytics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final CustomerRepository customerRepo;
    private final BillingRepository billingRepo;
    private final UsageRepository usageRepo;
    private final PaymentRepository paymentRepo;
    private final AnomalyRepository anomalyRepo;

    // @Scheduled(fixedDelay = 300_000L) // disabled during heavy ingest; trigger via /api/admin/detect
    public List<Anomaly> runAllRules() {
        List<Anomaly> found = new ArrayList<>();
        found.addAll(detectDuplicateCharges());
        found.addAll(detectBillingPaymentMismatch());
        found.addAll(detectUsageSpikes());
        found.addAll(detectFailedPaymentPatterns());
        found.addAll(detectRevenueLeakage());
        found.addAll(detectHighRiskChurnSignal());

        List<Anomaly> saved = anomalyRepo.saveAll(found);
        log.info("Anomaly scan complete: {} new anomalies", saved.size());
        return saved;
    }

    public List<Anomaly> detectDuplicateCharges() {
        Map<Long, List<PaymentRecord>> byBilling = paymentRepo.findAll().stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()) && p.getBillingId() != null)
                .collect(Collectors.groupingBy(PaymentRecord::getBillingId));

        List<Anomaly> out = new ArrayList<>();
        for (Map.Entry<Long, List<PaymentRecord>> e : byBilling.entrySet()) {
            List<PaymentRecord> ps = e.getValue();
            if (ps.size() < 2) continue;
            ps.sort(Comparator.comparing(PaymentRecord::getPaidAt));
            for (int i = 1; i < ps.size(); i++) {
                if (Duration.between(ps.get(i - 1).getPaidAt(), ps.get(i).getPaidAt())
                        .toMinutes() <= 10) {
                    out.add(Anomaly.builder()
                            .type("DUPLICATE_CHARGE")
                            .severity("HIGH")
                            .customerId(ps.get(i).getCustomerId())
                            .relatedEntity("PAYMENT")
                            .relatedId(ps.get(i).getId())
                            .description("Duplicate SUCCESS payment for billing "
                                    + e.getKey() + " within 10 minutes")
                            .status("OPEN")
                            .detectedAt(Instant.now())
                            .build());
                }
            }
        }
        return out;
    }

    public List<Anomaly> detectBillingPaymentMismatch() {
        List<Anomaly> out = new ArrayList<>();
        for (BillingRecord b : billingRepo.findAll()) {
            BigDecimal paid = paymentRepo.findByBillingId(b.getId()).stream()
                    .filter(p -> "SUCCESS".equals(p.getStatus()))
                    .map(PaymentRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (paid.signum() == 0) continue;
            if (paid.subtract(b.getAmount()).abs().compareTo(BigDecimal.ONE) > 0) {
                out.add(Anomaly.builder()
                        .type("BILLING_DISCREPANCY")
                        .severity("MEDIUM")
                        .customerId(b.getCustomerId())
                        .relatedEntity("BILLING")
                        .relatedId(b.getId())
                        .description("Billed " + b.getAmount() + " but paid " + paid)
                        .status("OPEN")
                        .detectedAt(Instant.now())
                        .build());
            }
        }
        return out;
    }

    public List<Anomaly> detectUsageSpikes() {
        List<Anomaly> out = new ArrayList<>();
        for (Customer c : customerRepo.findAll()) {
            List<UsageRecord> records = usageRepo.findByCustomerId(c.getId());
            Map<String, Double> meanByType = records.stream()
                    .collect(Collectors.groupingBy(
                            UsageRecord::getUsageType,
                            Collectors.averagingDouble(UsageRecord::getUnits)));
            for (UsageRecord u : records) {
                Double mean = meanByType.get(u.getUsageType());
                if (mean != null && mean > 0 && u.getUnits() > 3 * mean) {
                    out.add(Anomaly.builder()
                            .type("UNUSUAL_USAGE_SPIKE")
                            .severity("MEDIUM")
                            .customerId(c.getId())
                            .relatedEntity("USAGE")
                            .relatedId(u.getId())
                            .description(u.getUsageType() + " usage " + u.getUnits()
                                    + " exceeds 3x mean "
                                    + String.format("%.2f", mean))
                            .status("OPEN")
                            .detectedAt(Instant.now())
                            .build());
                }
            }
        }
        return out;
    }

    public List<Anomaly> detectFailedPaymentPatterns() {
        Instant cutoff = Instant.now().minusSeconds(30L * 86400L);
        Map<Long, Long> failsByCustomer = paymentRepo.findAll().stream()
                .filter(p -> "FAILED".equals(p.getStatus())
                        && p.getPaidAt() != null
                        && p.getPaidAt().isAfter(cutoff))
                .collect(Collectors.groupingBy(
                        PaymentRecord::getCustomerId, Collectors.counting()));

        return failsByCustomer.entrySet().stream()
                .filter(e -> e.getValue() >= 3)
                .map(e -> Anomaly.builder()
                        .type("PAYMENT_FAILURE_PATTERN")
                        .severity("HIGH")
                        .customerId(e.getKey())
                        .relatedEntity("CUSTOMER")
                        .relatedId(e.getKey())
                        .description(e.getValue() + " failed payments in last 30 days")
                        .status("OPEN")
                        .detectedAt(Instant.now())
                        .build())
                .collect(Collectors.toList());
    }

    public List<Anomaly> detectRevenueLeakage() {
        Instant cutoff = Instant.now().minusSeconds(30L * 86400L);
        List<Anomaly> out = new ArrayList<>();
        for (Customer c : customerRepo.findAll()) {
            double recentUnits = usageRepo
                    .findByCustomerIdAndOccurredAtAfter(c.getId(), cutoff).stream()
                    .mapToDouble(UsageRecord::getUnits).sum();
            boolean billedRecently = billingRepo.findByCustomerId(c.getId()).stream()
                    .anyMatch(b -> b.getBillingDate().isAfter(cutoff));
            if (recentUnits > 100 && !billedRecently) {
                out.add(Anomaly.builder()
                        .type("REVENUE_LEAKAGE")
                        .severity("CRITICAL")
                        .customerId(c.getId())
                        .relatedEntity("CUSTOMER")
                        .relatedId(c.getId())
                        .description("Usage of " + String.format("%.1f", recentUnits)
                                + " units in 30d with no billing record")
                        .status("OPEN")
                        .detectedAt(Instant.now())
                        .build());
            }
        }
        return out;
    }

    public List<Anomaly> detectHighRiskChurnSignal() {
        List<Anomaly> out = new ArrayList<>();
        Set<Long> alreadyFlagged = anomalyRepo.findAll().stream()
                .filter(a -> "HIGH_RISK_CHURN_SIGNAL".equals(a.getType()))
                .map(Anomaly::getCustomerId)
                .collect(Collectors.toSet());

        for (Customer c : customerRepo.findAll()) {
            if (!"HIGH".equals(c.getRiskTier())) continue;
            if (alreadyFlagged.contains(c.getId())) continue;

            long failedPayments = paymentRepo.findByCustomerId(c.getId()).stream()
                    .filter(p -> "FAILED".equals(p.getStatus()))
                    .count();
            if (failedPayments == 0) continue;

            out.add(Anomaly.builder()
                    .type("HIGH_RISK_CHURN_SIGNAL")
                    .severity("HIGH")
                    .customerId(c.getId())
                    .relatedEntity("CUSTOMER")
                    .relatedId(c.getId())
                    .description("HIGH-tier customer with " + failedPayments
                            + " failed payment(s); strong churn / collections risk")
                    .status("OPEN")
                    .detectedAt(Instant.now())
                    .build());
        }
        return out;
    }
}
