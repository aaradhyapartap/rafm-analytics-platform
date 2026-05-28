package com.rafm.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "anomalies",
        indexes = {
                @Index(name = "idx_anomaly_status",
                        columnList = "status,detected_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DUPLICATE_CHARGE, BILLING_DISCREPANCY, UNUSUAL_USAGE_SPIKE, etc. */
    private String type;

    /** LOW, MEDIUM, HIGH, CRITICAL. */
    private String severity;

    @Column(name = "customer_id")
    private Long customerId;

    /** BILLING / PAYMENT / USAGE / CUSTOMER. */
    private String relatedEntity;

    private Long relatedId;

    @Column(length = 1000)
    private String description;

    /** OPEN, INVESTIGATING, RESOLVED. */
    private String status;

    @Column(name = "detected_at")
    private Instant detectedAt;
}
