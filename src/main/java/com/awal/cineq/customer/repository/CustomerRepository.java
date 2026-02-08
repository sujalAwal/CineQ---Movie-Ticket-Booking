package com.awal.cineq.customer.repository;

import com.awal.cineq.customer.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Customer
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {

    // Find by email (no soft-delete filter - for authentication)
    Optional<Customer> findByEmail(String email);
    
    // Find active customer by email
    @Query("{ 'email': ?0, 'isActive': true, 'deletedAt': null }")
    Optional<Customer> findByEmailAndIsActiveTrue(String email);
    
    // Find by email verification token
    @Query("{ 'emailVerificationToken': ?0, 'deletedAt': null }")
    Optional<Customer> findByEmailVerificationToken(String token);
    
    // Find all active customers
    @Query("{ 'isActive': true, 'deletedAt': null }")
    List<Customer> findByIsActiveTrue();
    
    // Find all email verified customers
    @Query("{ 'isEmailVerified': true, 'deletedAt': null }")
    List<Customer> findByIsEmailVerifiedTrue();
    
    // Search customers by keyword (firstName, lastName, email)
    @Query("{ $and: [ " +
           "  { $or: [ " +
           "    { 'firstName': { $regex: ?0, $options: 'i' } }, " +
           "    { 'lastName': { $regex: ?0, $options: 'i' } }, " +
           "    { 'email': { $regex: ?0, $options: 'i' } } " +
           "  ] }, " +
           "  { 'isActive': true }, " +
           "  { 'deletedAt': null } " +
           "] }")
    List<Customer> searchCustomers(String keyword);

    // Check if email exists
    boolean existsByEmail(String email);
    
    // Count active customers
    @Query(value = "{ 'isActive': true, 'deletedAt': null }", count = true)
    Long countActiveCustomers();
    
    // Count verified customers
    @Query(value = "{ 'isEmailVerified': true, 'isActive': true, 'deletedAt': null }", count = true)
    Long countVerifiedCustomers();
    
    // Get total loyalty points (aggregation - implement in service layer)
    @Query("{ 'isActive': true, 'deletedAt': null }")
    List<Customer> findAllActiveCustomers();
    
    // Find by password reset token
    @Query("{ 'passwordResetToken': ?0, 'deletedAt': null }")
    Optional<Customer> findByPasswordResetToken(String token);
}