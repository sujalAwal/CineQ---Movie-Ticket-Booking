package com.awal.cineq.customer.controller;

import com.awal.cineq.customer.dto.CustomerResponse;
import com.awal.cineq.customer.dto.CustomerUpdateRequest;
import com.awal.cineq.customer.service.CustomerService;
import com.awal.cineq.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/frontend/customer")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/profile/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getProfile(@PathVariable String customerId) {  // Changed from UUID to String
        CustomerResponse customer = customerService.getProfile(customerId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", customer));
    }

    @PutMapping("/profile/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateProfile(
            @PathVariable String customerId,  // Changed from UUID to String
            @Valid @RequestBody CustomerUpdateRequest updateRequest) {
        
        CustomerResponse updatedCustomer = customerService.updateProfile(customerId, updateRequest);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedCustomer));
    }

    @DeleteMapping("/profile/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> deleteAccount(@PathVariable String customerId) {  // Changed from UUID to String
        customerService.deleteAccount(customerId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", "Your account has been deleted"));
    }

    // Admin endpoints for customer management
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable String customerId) {  // Changed from UUID to String
        CustomerResponse customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully", customer));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(@RequestParam String keyword) {
        List<CustomerResponse> customers = customerService.searchCustomers(keyword);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", customers));
    }
}