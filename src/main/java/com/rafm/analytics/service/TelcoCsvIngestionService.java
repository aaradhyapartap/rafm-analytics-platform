package com.rafm.analytics.service;

import com.rafm.analytics.model.*;
import com.rafm.analytics.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TelcoCsvIngestionService {

    private final CustomerRepository customerRepo;
    private final BillingRepository billingRepo;
    private final PaymentRepository paymentRepo;

    private static final int MAX_BILLING_ROWS_PER_CUSTOMER = 12;

    public Map<String, Integer> ingest(String csvPath, int maxRows) throws Exception {
        int customers = 0, billing = 0, payments = 0, skipped = 0;
        Random rnd = new Random(7);

        try (Reader reader = new FileReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord row : parser) {
                if (maxRows > 0 && customers >= maxRows) break;

                try {
                    String externalId = row.get("customerID");
                    String gender = row.get("gender");
                    int tenure = parseIntSafe(row.get("tenure"), 0);
                    String contract = row.get("Contract");
                    String paymentMethod = row.get("PaymentMethod");
                    BigDecimal monthlyCharges = parseDecimalSafe(row.get("MonthlyCharges"));
                    String churnFlag = row.get("Churn");

                    Customer c = customerRepo.save(Customer.builder()
                            .name("Telco-" + externalId + " (" + gender + ")")
                            .email(externalId.toLowerCase() + "@telco-import.example")
                            .phone("+1-555-" + (1000 + (Math.abs(externalId.hashCode()) % 9000)))
                            .accountStatus("Yes".equalsIgnoreCase(churnFlag) ? "CLOSED" : "ACTIVE")
                            .riskTier(deriveRiskTier(contract, paymentMethod, churnFlag))
                            .createdAt(Instant.now().minus(tenure * 30L, ChronoUnit.DAYS))
                            .build());
                    customers++;

                    int monthsToGenerate = Math.min(tenure, MAX_BILLING_ROWS_PER_CUSTOMER);
                    for (int m = 0; m < monthsToGenerate; m++) {
                        BillingRecord br = billingRepo.save(BillingRecord.builder()
                                .customerId(c.getId())
                                .amount(monthlyCharges)
                                .billingDate(Instant.now().minus(m * 30L, ChronoUnit.DAYS))
                                .status("ISSUED")
                                .invoiceNumber("INV-" + externalId + "-" + m)
                                .createdAt(Instant.now())
                                .build());
                        billing++;

                        boolean failed = "Yes".equalsIgnoreCase(churnFlag) && rnd.nextDouble() < 0.30;
                        paymentRepo.save(PaymentRecord.builder()
                                .customerId(c.getId())
                                .billingId(br.getId())
                                .amount(monthlyCharges)
                                .method(mapPaymentMethod(paymentMethod))
                                .status(failed ? "FAILED" : "SUCCESS")
                                .paidAt(br.getBillingDate().plus(2, ChronoUnit.DAYS))
                                .build());
                        payments++;
                    }
                } catch (Exception rowEx) {
                    skipped++;
                    log.warn("Skipping row {}: {}", row.getRecordNumber(), rowEx.getMessage());
                }
            }
        }

        log.info("Telco CSV ingest complete: customers={} billing={} payments={} skipped={}",
                customers, billing, payments, skipped);
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("customers", customers);
        out.put("billing", billing);
        out.put("payments", payments);
        out.put("skipped", skipped);
        return out;
    }

    private String deriveRiskTier(String contract, String paymentMethod, String churn) {
        boolean monthly = "Month-to-month".equalsIgnoreCase(contract);
        boolean eCheck = "Electronic check".equalsIgnoreCase(paymentMethod);
        boolean churned = "Yes".equalsIgnoreCase(churn);
        if (monthly && eCheck && churned) return "HIGH";
        if (monthly || churned) return "MEDIUM";
        return "LOW";
    }

    private String mapPaymentMethod(String src) {
        if (src == null) return "CARD";
        String s = src.toLowerCase();
        if (s.contains("credit") || s.contains("card")) return "CARD";
        if (s.contains("bank") || s.contains("check")) return "ACH";
        return "WALLET";
    }

    private int parseIntSafe(String v, int fallback) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return fallback; }
    }

    private BigDecimal parseDecimalSafe(String v) {
        try {
            return new BigDecimal(v.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
