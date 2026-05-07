package com.rafm.analytics.repository;

import com.rafm.analytics.model.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {
    List<Anomaly> findByStatus(String status);
}
