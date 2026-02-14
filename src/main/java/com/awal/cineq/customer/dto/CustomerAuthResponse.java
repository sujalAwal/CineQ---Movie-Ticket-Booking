package com.awal.cineq.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Customer authentication response DTO
 * MongoDB compatible: uses String ID instead of UUID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAuthResponse {
    private String token;
    private String type;
    private String id;  // MongoDB ObjectId as String
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private Integer loyaltyPoints;
    private Boolean isEmailVerified;
    private String role;
}