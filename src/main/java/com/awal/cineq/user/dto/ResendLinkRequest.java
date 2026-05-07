package com.awal.cineq.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendLinkRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email format")
    private String email;
}
