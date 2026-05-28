package com.rafm.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "investigation_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anomaly_id")
    private Long anomalyId;

    private String classification;

    @Lob
    private String evidenceJson;

    @Column(length = 500)
    private String recommendedAction;

    private Double confidenceScore;

    @Lob
    private String reportText;

    private Instant generatedAt;
}
