package com.awal.cineq.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user details.
 * Password update is handled separately for security reasons.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Email(message = "Please provide a valid email address")
    @Size(max = 200, message = "Email must not exceed 200 characters")
    private String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    /**
     * Reference to the roles collection document ID.
     */
    private String roleId;

    /**
     * Whether the user is active or not.
     */
    private Boolean isActive;
}
