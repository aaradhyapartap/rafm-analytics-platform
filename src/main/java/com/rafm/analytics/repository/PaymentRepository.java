package com.rafm.analytics.repository;

import com.rafm.analytics.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByCustomerId(Long customerId);
    List<PaymentRecord> findByBillingId(Long billingId);
}
