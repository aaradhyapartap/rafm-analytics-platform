package com.rafm.analytics.service.agent;

import com.rafm.analytics.model.*;
import com.rafm.analytics.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AgentTools {

    private final CustomerRepository customerRepo;
    private final BillingRepository billingRepo;
    private final UsageRepository usageRepo;
    private final PaymentRepository paymentRepo;

    public Optional<Customer> customerLookup(Long customerId) {
        return customerRepo.findById(customerId);
    }

    public List<BillingRecord> billingLookup(Long customerId) {
        return billingRepo.findByCustomerId(customerId);
    }

    public List<PaymentRecord> paymentLookup(Long customerId) {
        return paymentRepo.findByCustomerId(customerId);
    }

    public List<UsageRecord> usageLookup(Long customerId) {
        return usageRepo.findByCustomerId(customerId);
    }

    public double riskScore(Anomaly a, Customer customer) {
        double base = switch (a.getType()) {
            case "DUPLICATE_CHARGE" -> 0.7;
            case "BILLING_DISCREPANCY" -> 0.5;
            case "UNUSUAL_USAGE_SPIKE" -> 0.4;
            case "PAYMENT_FAILURE_PATTERN" -> 0.75;
            case "REVENUE_LEAKAGE" -> 0.9;
            case "HIGH_RISK_CHURN_SIGNAL" -> 0.85;
            default -> 0.3;
        };
        double tierBoost = customer == null ? 0
                : switch (customer.getRiskTier() == null ? "LOW" : customer.getRiskTier()) {
                    case "HIGH" -> 0.15;
                    case "MEDIUM" -> 0.05;
                    default -> 0.0;
                };
        return Math.min(1.0, base + tierBoost);
    }

    public String classify(Anomaly a) {
        return switch (a.getType()) {
            case "DUPLICATE_CHARGE" ->
                    "Billing integrity issue - duplicate transaction";
            case "BILLING_DISCREPANCY" ->
                    "Reconciliation issue - payment differs from billed amount";
            case "UNUSUAL_USAGE_SPIKE" ->
                    "Behavioral anomaly - potential fraud or rate-plan misuse";
            case "PAYMENT_FAILURE_PATTERN" ->
                    "Credit risk - recurring payment failures";
            case "REVENUE_LEAKAGE" ->
                    "Revenue assurance gap - unbilled usage";
            case "HIGH_RISK_CHURN_SIGNAL" ->
                    "Credit & retention risk - high-tier customer with payment failures";
            default -> "Unclassified anomaly";
        };
    }

    public String recommend(Anomaly a, double risk) {
        if (risk >= 0.8) {
            return "Escalate to fraud ops; freeze related transactions pending review.";
        }
        if (risk >= 0.5) {
            return "Open analyst case; verify with customer within 48h.";
        }
        return "Log for trend monitoring; review in weekly RA report.";
    }
}
