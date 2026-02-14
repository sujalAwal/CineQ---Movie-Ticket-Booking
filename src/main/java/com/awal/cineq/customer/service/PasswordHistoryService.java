package com.awal.cineq.customer.service;

import com.awal.cineq.customer.model.PasswordHistory;
import com.awal.cineq.customer.repository.PasswordHistoryRepository;
import com.awal.cineq.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing password history
 * Prevents users from reusing recent passwords
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordHistoryService {
    
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    
    private static final int MAX_HISTORY_COUNT = 5;
    private static final int EXPIRY_DAYS = 90;
    
    /**
     * Validate that a new password is not in the user's recent password history
     * 
     * @param customerId Customer ID
     * @param newPassword Plain text new password
     * @throws BadRequestException if password has been used recently
     */
    public void validatePasswordNotReused(String customerId, String newPassword) {
        List<PasswordHistory> history = passwordHistoryRepository
            .findTop5ByCustomerIdOrderByCreatedAtDesc(customerId);
        
        for (PasswordHistory entry : history) {
            if (passwordEncoder.matches(newPassword, entry.getPasswordHash())) {
                throw new BadRequestException(
                    "Password has been used recently. Please choose a different password."
                );
            }
        }
        
        log.debug("Password validation passed for customer: {}", customerId);
    }
    
    /**
     * Add a password to the customer's password history
     * Automatically maintains max history count by deleting oldest entries
     * 
     * @param customerId Customer ID
     * @param passwordHash Bcrypt hashed password
     */
    public void addPasswordToHistory(String customerId, String passwordHash) {
        // Create new history entry
        PasswordHistory entry = new PasswordHistory();
        entry.setCustomerId(customerId);
        entry.setPasswordHash(passwordHash);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setExpiresAt(LocalDateTime.now().plusDays(EXPIRY_DAYS));
        
        passwordHistoryRepository.save(entry);
        log.info("Password added to history for customer: {}", customerId);
        
        // Keep only last MAX_HISTORY_COUNT passwords
        List<PasswordHistory> allHistory = passwordHistoryRepository
            .findByCustomerIdOrderByCreatedAtDesc(customerId);
        
        if (allHistory.size() > MAX_HISTORY_COUNT) {
            List<PasswordHistory> toDelete = allHistory.subList(MAX_HISTORY_COUNT, allHistory.size());
            passwordHistoryRepository.deleteAll(toDelete);
            log.debug("Cleaned up old password history entries for customer: {}. Deleted {} entries", 
                customerId, toDelete.size());
        }
    }
    
    /**
     * Get password history count for a customer
     * 
     * @param customerId Customer ID
     * @return Number of password history entries
     */
    public long getHistoryCount(String customerId) {
        return passwordHistoryRepository.countByCustomerId(customerId);
    }
    
    /**
     * Clear all password history for a customer (for admin/testing purposes)
     * 
     * @param customerId Customer ID
     */
    public void clearHistory(String customerId) {
        List<PasswordHistory> history = passwordHistoryRepository
            .findByCustomerIdOrderByCreatedAtDesc(customerId);
        
        passwordHistoryRepository.deleteAll(history);
        log.info("Password history cleared for customer: {}", customerId);
    }
}
