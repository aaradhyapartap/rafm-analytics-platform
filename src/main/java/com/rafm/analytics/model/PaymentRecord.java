package com.rafm.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_records",
        indexes = {
                @Index(name = "idx_payment_billing", columnList = "billing_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    /** Nullable: a payment may not be linked to a specific billing record. */
    @Column(name = "billing_id")
    private Long billingId;

    private BigDecimal amount;

    /** CARD, ACH, WALLET. */
    private String method;

    /** SUCCESS, FAILED, PENDING, REFUNDED. */
    private String status;

    private Instant paidAt;
}
