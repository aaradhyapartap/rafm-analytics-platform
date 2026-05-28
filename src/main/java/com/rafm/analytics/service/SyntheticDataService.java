package com.rafm.analytics.service;

import com.rafm.analytics.model.*;
import com.rafm.analytics.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SyntheticDataService {

    private final CustomerRepository customerRepo;
    private final BillingRepository billingRepo;
    private final UsageRepository usageRepo;
    private final PaymentRepository paymentRepo;

    private final Random rnd = new Random(42);

    public Map<String, Integer> seed(int numCustomers) {
        int billing = 0, usage = 0, payments = 0;
        List<String> tiers = List.of("LOW", "MEDIUM", "HIGH");
        List<String> usageTypes = List.of("VOICE", "DATA", "SMS", "ROAMING");
        List<String> locations = List.of("US-CA", "US-NY", "US-TX", "IN-MH", "DE-BY");

        for (int i = 0; i < numCustomers; i++) {
            Customer c = customerRepo.save(Customer.builder()
                    .name("Customer " + (i + 1))
                    .email("user" + (i + 1) + "@example.com")
                    .phone("+1-555-" + (1000 + i))
                    .accountStatus("ACTIVE")
                    .riskTier(tiers.get(rnd.nextInt(tiers.size())))
                    .createdAt(Instant.now().minus(rnd.nextInt(365), ChronoUnit.DAYS))
                    .build());

            for (int b = 0; b < 3; b++) {
                BigDecimal amt = BigDecimal.valueOf(50 + rnd.nextInt(300));
                BillingRecord br = billingRepo.save(BillingRecord.builder()
                        .customerId(c.getId())
                        .amount(amt)
                        .billingDate(Instant.now().minus(b * 30L, ChronoUnit.DAYS))
                        .status("ISSUED")
                        .invoiceNumber("INV-" + c.getId() + "-" + b)
                        .createdAt(Instant.now())
                        .build());
                billing++;

                if (rnd.nextDouble() > 0.15) {
                    BigDecimal payAmt = (rnd.nextDouble() > 0.05)
                            ? amt
                            : amt.subtract(BigDecimal.valueOf(rnd.nextInt(20)));
                    paymentRepo.save(PaymentRecord.builder()
                            .customerId(c.getId())
                            .billingId(br.getId())
                            .amount(payAmt)
                            .method("CARD")
                            .status("SUCCESS")
                            .paidAt(br.getBillingDate().plus(2, ChronoUnit.DAYS))
                            .build());
                    payments++;
                }

                if (rnd.nextDouble() < 0.05) {
                    paymentRepo.save(PaymentRecord.builder()
                            .customerId(c.getId())
                            .billingId(br.getId())
                            .amount(amt)
                            .method("CARD")
                            .status("SUCCESS")
                            .paidAt(br.getBillingDate()
                                    .plus(2, ChronoUnit.DAYS)
                                    .plus(3, ChronoUnit.MINUTES))
                            .build());
                    payments++;
                }
            }

            int n = 20 + rnd.nextInt(30);
            for (int u = 0; u < n; u++) {
                double units = 1 + rnd.nextDouble() * 50;
                if (rnd.nextDouble() < 0.02) units *= 10;
                usageRepo.save(UsageRecord.builder()
                        .customerId(c.getId())
                        .usageType(usageTypes.get(rnd.nextInt(usageTypes.size())))
                        .units(units)
                        .location(locations.get(rnd.nextInt(locations.size())))
                        .occurredAt(Instant.now().minus(rnd.nextInt(60), ChronoUnit.DAYS))
                        .build());
                usage++;
            }
        }

        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("customers", numCustomers);
        out.put("billing", billing);
        out.put("usage", usage);
        out.put("payments", payments);
        return out;
    }
}
