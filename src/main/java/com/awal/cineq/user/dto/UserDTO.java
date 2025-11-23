package com.awal.cineq.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.awal.cineq.user.model.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 200, message = "Email must not exceed 200 characters")
    private String email;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
    
    private User.UserRole role;
    private Boolean isActive;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}