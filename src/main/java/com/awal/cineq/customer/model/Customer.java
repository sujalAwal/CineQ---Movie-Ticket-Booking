package com.awal.cineq.customer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MongoDB Document for Customer
 * Uses soft-delete pattern: deletedAt = null means active, not null means deleted
 */
@Document(collection = "customers")
@CompoundIndexes({
    @CompoundIndex(name = "email_active_idx", def = "{'email': 1, 'deletedAt': 1}", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    private String id;  // MongoDB ObjectId stored as String

    @Field("first_name")
    private String firstName;
    
    @Field("middle_name")
    private String middleName;
    
    @Field("last_name")
    private String lastName;
    
    @Field("email")
    private String email;  // Uniqueness enforced by compound index (email, deletedAt)

    @Field("password")
    private String password;
    
    @Field("phone")
    private String phone;

    @Field("profile_picture")
    private String profilePicture;

    @Field("previous_profile_picture")
    private String previousProfilePicture;
    
    @Field("date_of_birth")
    private LocalDate dateOfBirth;
    
    @Field("gender")
    private Gender gender;
    
    @Field("loyalty_points")
    private Integer loyaltyPoints = 0;
    
    @Field("is_email_verified")
    private Boolean isEmailVerified = false;
    
    @Field("email_verification_token")
    private String emailVerificationToken;
    
    @Field("email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;
    
    // Login attempt tracking (for lockout)
    @Field("failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Field("locked_until")
    private LocalDateTime lockedUntil;
    
    @Field("last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;
    
    // Password reset
    @Field("password_reset_token")
    private String passwordResetToken;
    
    @Field("password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;
    
    @Field("is_active")
    private Boolean isActive = true;
    
    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Field("deleted_at")
    private LocalDateTime deletedAt;  // Soft-delete marker: null = active

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }
    
    public void addLoyaltyPoints(Integer points) {
        this.loyaltyPoints = this.loyaltyPoints + points;
    }
    
    public void deductLoyaltyPoints(Integer points) {
        this.loyaltyPoints = Math.max(0, this.loyaltyPoints - points);
    }
    
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
        this.lastFailedLoginAt = LocalDateTime.now();
    }
    
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastFailedLoginAt = null;
    }
    
    public boolean isLocked() {
        return this.lockedUntil != null && LocalDateTime.now().isBefore(this.lockedUntil);
    }
    
    public void lockAccount(int durationMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
    }
    
    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }
}