package com.awal.cineq.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;
}
