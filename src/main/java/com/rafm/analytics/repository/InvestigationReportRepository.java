package com.rafm.analytics.repository;

import com.rafm.analytics.model.InvestigationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvestigationReportRepository extends JpaRepository<InvestigationReport, Long> {
    Optional<InvestigationReport> findByAnomalyId(Long anomalyId);
}
