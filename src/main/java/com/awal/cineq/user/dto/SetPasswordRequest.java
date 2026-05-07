package com.awal.cineq.user.dto;

import com.awal.cineq.customer.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetPasswordRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;
}
