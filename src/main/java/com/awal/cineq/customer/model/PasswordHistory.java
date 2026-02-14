package com.awal.cineq.customer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB Document for tracking password history
 * Prevents users from reusing recent passwords
 */
@Document(collection = "password_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHistory {
    
    @Id
    private String id;
    
    @Field("customer_id")
    @Indexed
    private String customerId;
    
    @Field("password_hash")
    private String passwordHash;  // Stored as bcrypt hash
    
    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Field("expires_at")
    private LocalDateTime expiresAt;  // 90 days from creation
}
