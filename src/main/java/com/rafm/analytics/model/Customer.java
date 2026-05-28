package com.rafm.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;

    /** ACTIVE, SUSPENDED, CLOSED. */
    private String accountStatus;

    /** LOW, MEDIUM, HIGH. */
    private String riskTier;

    private Instant createdAt;
}
