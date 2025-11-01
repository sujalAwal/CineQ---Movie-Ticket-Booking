package com.awal.cineq.customer.service;

import com.awal.cineq.customer.dto.CustomerResponse;
import com.awal.cineq.customer.dto.CustomerUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse getProfile(UUID customerId);
    CustomerResponse updateProfile(UUID customerId, CustomerUpdateRequest updateRequest);
    void deleteAccount(UUID customerId);
    List<CustomerResponse> getAllCustomers();
    CustomerResponse getCustomerById(UUID customerId);
    List<CustomerResponse> searchCustomers(String keyword);
}