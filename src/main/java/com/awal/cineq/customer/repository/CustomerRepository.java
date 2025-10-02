package com.awal.cineq.customer.repository;

import com.awal.cineq.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    Optional<Customer> findByEmail(String email);
    
    Optional<Customer> findByEmailAndIsActiveTrue(String email);
    
    Optional<Customer> findByEmailVerificationToken(String token);
    
    List<Customer> findByIsActiveTrue();
    
    List<Customer> findByIsEmailVerifiedTrue();
    
    @Query("SELECT c FROM Customer c WHERE (c.firstName LIKE %:keyword% OR c.lastName LIKE %:keyword% OR c.email LIKE %:keyword%) AND c.isActive = true")
    List<Customer> searchCustomers(@Param("keyword") String keyword);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isActive = true")
    Long countActiveCustomers();
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isEmailVerified = true AND c.isActive = true")
    Long countVerifiedCustomers();
    
    @Query("SELECT SUM(c.loyaltyPoints) FROM Customer c WHERE c.isActive = true")
    Long getTotalLoyaltyPoints();
}