package com.awal.cineq.customer.repository;

import com.awal.cineq.customer.model.PasswordHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB Repository for PasswordHistory
 */
@Repository
public interface PasswordHistoryRepository extends MongoRepository<PasswordHistory, String> {
    
    /**
     * Find top N password history entries for a customer, ordered by creation date descending
     * 
     * @param customerId Customer ID
     * @return List of password history entries (most recent first)
     */
    List<PasswordHistory> findTop5ByCustomerIdOrderByCreatedAtDesc(String customerId);
    
    /**
     * Find all password history entries for a customer, ordered by creation date descending
     * 
     * @param customerId Customer ID
     * @return List of password history entries (most recent first)
     */
    List<PasswordHistory> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    
    /**
     * Count password history entries for a customer
     * 
     * @param customerId Customer ID
     * @return Count of password history entries
     */
    long countByCustomerId(String customerId);
}
