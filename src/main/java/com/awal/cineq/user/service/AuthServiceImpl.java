package com.awal.cineq.user.service;

import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.user.dto.AuthResponse;
import com.awal.cineq.user.dto.LoginRequest;
import com.awal.cineq.user.dto.RegisterRequest;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmailAndIsActiveTrue(loginRequest.getEmail())
                    .orElseThrow(() -> new BadRequestException("User not found"));

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

            log.info("User {} logged in successfully", user.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .build();

        } catch (Exception e) {
            log.error("Login failed for email: {}", loginRequest.getEmail(), e);
            throw new BadRequestException("Invalid email or password");
        }
    }

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        if(null == registerRequest.getPassword() || registerRequest.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }


        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(registerRequest.getRole() != null ? registerRequest.getRole() : User.UserRole.USER);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        log.info("User {} registered successfully", savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .build();
    }

    @Override
    public void logout(String token) {
        // In a production environment, you might want to maintain a blacklist of tokens
        // For now, we'll just clear the security context
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }
}