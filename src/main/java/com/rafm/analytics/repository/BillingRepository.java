package com.rafm.analytics.repository;

import com.rafm.analytics.model.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillingRepository extends JpaRepository<BillingRecord, Long> {
    List<BillingRecord> findByCustomerId(Long customerId);
}
