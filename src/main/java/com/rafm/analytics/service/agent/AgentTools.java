package com.rafm.analytics.service.agent;

import com.rafm.analytics.model.Anomaly;
import com.rafm.analytics.model.Customer;
import com.rafm.analytics.repository.BillingRepository;
import com.rafm.analytics.repository.CustomerRepository;
import com.rafm.analytics.repository.PaymentRepository;
import com.rafm.analytics.repository.UsageRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AgentTools {

    private final CustomerRepository customerRepository;
    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final UsageRepository usageRepository;

    public AgentTools(
            CustomerRepository customerRepository,
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            UsageRepository usageRepository
    ) {
        this.customerRepository = customerRepository;
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.usageRepository = usageRepository;
    }

    public Map<String, Object> gatherEvidence(Anomaly anomaly) {
        Map<String, Object> evidence = new HashMap<>();

        Customer customer = customerRepository.findById(anomaly.getCustomerId()).orElse(null);

        evidence.put("customer", customer);
        evidence.put("billingRecords", billingRepository.findByCustomerId(anomaly.getCustomerId()));
        evidence.put("paymentRecords", paymentRepository.findByCustomerId(anomaly.getCustomerId()));
        evidence.put("usageRecords", usageRepository.findByCustomerId(anomaly.getCustomerId()));

        return evidence;
    }

    public String classify(Anomaly anomaly) {
        if ("DUPLICATE_PAYMENT".equalsIgnoreCase(anomaly.getType())) {
            return "Revenue assurance issue: duplicate payment activity detected.";
        }

        if ("BILLING_PAYMENT_MISMATCH".equalsIgnoreCase(anomaly.getType())) {
            return "Billing reconciliation issue: payment total does not match invoice amount.";
        }

        return "General anomaly requiring analyst review.";
    }

    public double scoreRisk(Anomaly anomaly) {
        if ("HIGH".equalsIgnoreCase(anomaly.getSeverity())) {
            return 0.85;
        }

        if ("MEDIUM".equalsIgnoreCase(anomaly.getSeverity())) {
            return 0.60;
        }

        return 0.35;
    }

    public String recommendAction(Anomaly anomaly, double riskScore) {
        if (riskScore >= 0.80) {
            return "Escalate to revenue assurance analyst and review duplicate transaction trail.";
        }

        if (riskScore >= 0.50) {
            return "Open a reconciliation case and verify billing/payment records.";
        }

        return "Monitor trend and include in weekly revenue assurance review.";
    }
}
