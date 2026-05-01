package com.awal.cineq.customer.dto;

import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegisterRequest {
    
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Middle name must not exceed 100 characters")
    private String middleName;  // Optional field
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 200, message = "Email must not exceed 200 characters")
    private String email;
    
    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @Pattern(
        regexp = "^[0-9+\\-\\(\\)\\s]{7,20}$",
        message = "Phone number must contain 7-20 characters and only digits, +, -, (, ), and spaces"
    )
    @Size(min = 7, max = 20, message = "Phone number must be between 7 and 20 characters")
    private String phone;
    
    private LocalDate dateOfBirth;
    
    private Customer.Gender gender;
}