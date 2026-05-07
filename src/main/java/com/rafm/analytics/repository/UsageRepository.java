package com.rafm.analytics.repository;

import com.rafm.analytics.model.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface UsageRepository extends JpaRepository<UsageRecord, Long> {
    List<UsageRecord> findByCustomerId(Long customerId);
    List<UsageRecord> findByCustomerIdAndOccurredAtAfter(Long customerId, Instant after);
}
