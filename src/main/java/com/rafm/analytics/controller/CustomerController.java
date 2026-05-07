package com.rafm.analytics.controller;

import com.rafm.analytics.model.Customer;
import com.rafm.analytics.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(Instant.now());
        }
        if (customer.getAccountStatus() == null) {
            customer.setAccountStatus("ACTIVE");
        }
        if (customer.getRiskTier() == null) {
            customer.setRiskTier("LOW");
        }
        return customerRepository.save(customer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
        return customerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Iterable<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}
