package com.awal.cineq.customer.service;

import com.awal.cineq.customer.dto.CustomerResponse;
import com.awal.cineq.customer.dto.CustomerUpdateRequest;

import java.util.List;

/**
 * Customer Service Interface
 * MongoDB compatible: uses String ID instead of UUID
 */
public interface CustomerService {
    CustomerResponse getProfile(String customerId);
    CustomerResponse updateProfile(String customerId, CustomerUpdateRequest updateRequest);
    void deleteAccount(String customerId);
    List<CustomerResponse> getAllCustomers();
    CustomerResponse getCustomerById(String customerId);
    List<CustomerResponse> searchCustomers(String keyword);
}