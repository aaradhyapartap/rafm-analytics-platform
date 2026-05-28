package com.rafm.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "billing_records",
        indexes = {
                @Index(name = "idx_billing_customer_date",
                        columnList = "customer_id,billing_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    private BigDecimal amount;

    @Column(name = "billing_date")
    private Instant billingDate;

    /** ISSUED, PAID, OVERDUE, VOID. */
    private String status;

    private String invoiceNumber;

    private Instant createdAt;
}
