package com.awal.cineq.customer.service;

import com.awal.cineq.customer.dto.CustomerResponse;
import com.awal.cineq.customer.dto.CustomerUpdateRequest;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponse getProfile(UUID customerId) {
        Customer customer = findCustomerById(customerId);
        return mapToCustomerResponse(customer);
    }

    @Override
    public CustomerResponse updateProfile(UUID customerId, CustomerUpdateRequest updateRequest) {
        Customer customer = findCustomerById(customerId);

        // Check if email is being changed and if it already exists
        if (updateRequest.getEmail() != null && 
            !updateRequest.getEmail().equals(customer.getEmail()) && 
            customerRepository.existsByEmail(updateRequest.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Update fields if they are provided
        if (updateRequest.getFirstName() != null) {
            customer.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            customer.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getEmail() != null) {
            customer.setEmail(updateRequest.getEmail());
            // If email is changed, mark as unverified
            if (!updateRequest.getEmail().equals(customer.getEmail())) {
                customer.setIsEmailVerified(false);
            }
        }
        if (updateRequest.getPhone() != null) {
            customer.setPhone(updateRequest.getPhone());
        }
        if (updateRequest.getDateOfBirth() != null) {
            customer.setDateOfBirth(updateRequest.getDateOfBirth());
        }
        if (updateRequest.getGender() != null) {
            customer.setGender(updateRequest.getGender());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Customer profile updated for: {}", customer.getEmail());

        return mapToCustomerResponse(updatedCustomer);
    }

    @Override
    public void deleteAccount(UUID customerId) {
        Customer customer = findCustomerById(customerId);
        customer.softDelete();
        customerRepository.save(customer);
        log.info("Customer account deleted for: {}", customer.getEmail());
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponse getCustomerById(UUID customerId) {
        Customer customer = findCustomerById(customerId);
        return mapToCustomerResponse(customer);
    }

    @Override
    public List<CustomerResponse> searchCustomers(String keyword) {
        return customerRepository.searchCustomers(keyword)
                .stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }

    private Customer findCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .dateOfBirth(customer.getDateOfBirth())
                .gender(customer.getGender())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .isEmailVerified(customer.getIsEmailVerified())
                .isActive(customer.getIsActive())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}