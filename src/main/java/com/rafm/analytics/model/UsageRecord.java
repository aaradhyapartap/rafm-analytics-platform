package com.rafm.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "usage_records",
        indexes = {
                @Index(name = "idx_usage_customer_time",
                        columnList = "customer_id,occurred_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    /** VOICE, DATA, SMS, ROAMING. */
    private String usageType;

    /** minutes / MB / count, depending on usageType. */
    private Double units;

    private String location;

    @Column(name = "occurred_at")
    private Instant occurredAt;
}
