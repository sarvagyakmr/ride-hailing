package com.ridehailing.ridematch.controller;

import com.ridehailing.ridematch.dto.CustomerResponse;
import com.ridehailing.ridematch.entity.Customer;
import com.ridehailing.ridematch.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer() {
        Customer savedCustomer = customerService.createCustomer();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerResponse.fromEntity(savedCustomer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable("id") Long customerId) {
        return customerService.getCustomerById(customerId)
                .map(customer -> ResponseEntity.ok(CustomerResponse.fromEntity(customer)))
                .orElse(ResponseEntity.notFound().build());
    }
}