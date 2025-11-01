package com.awal.cineq.user.service;

import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.user.dto.AuthResponse;
import com.awal.cineq.user.dto.LoginRequest;
import com.awal.cineq.user.dto.RegisterRequest;
import com.awal.cineq.user.dto.UserDTO;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.mapper.Mapper;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper  modelMapper;
    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("AuthServiceImpl: Starting login process for email: {}", loginRequest.getEmail());

        try {
            // Find user by email
            User user = userRepository.findByEmailAndIsActiveTrue(loginRequest.getEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new BadRequestException("Invalid email or password");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

            log.info("User {} logged in successfully", user.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(modelMapper.map(user, UserDTO.class))
                    .build();

        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequest.getEmail(), e);
            throw new BadRequestException("Invalid email or password");
        }
    }

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user with email: {}", registerRequest.getEmail());

        if (registerRequest.getPassword() == null || registerRequest.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        // Create user
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : User.UserRole.USER);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        log.info("User {} registered successfully", savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(modelMapper.map(savedUser, UserDTO.class))
                .build();
    }

    @Override
    public void logout(String token) {
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }
}

