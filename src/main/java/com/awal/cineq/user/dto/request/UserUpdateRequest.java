package com.awal.cineq.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
     * Role document IDs to assign (replaces active assignments in user_has_roles).
     * Required on update; at least one role.
     */
    @NotEmpty(message = "At least one role is required")
    private List<@NotBlank(message = "Role id must not be blank") String> roleIds;

    /**
     * Whether the user is active or not.
     */
    private Boolean isActive;
}
