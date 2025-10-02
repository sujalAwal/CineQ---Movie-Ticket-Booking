package com.awal.cineq.customer.service;

import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.customer.dto.*;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    public CustomerAuthResponse login(CustomerLoginRequest loginRequest) {
        try {
            // Check if customer exists and is active
            Customer customer = customerRepository.findByEmailAndIsActiveTrue(loginRequest.getEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));

            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtUtil.generateToken(customer.getEmail(), "CUSTOMER");

            log.info("Customer {} logged in successfully", customer.getEmail());

            return CustomerAuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .id(customer.getId())
                    .email(customer.getEmail())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .loyaltyPoints(customer.getLoyaltyPoints())
                    .isEmailVerified(customer.getIsEmailVerified())
                    .role("CUSTOMER")
                    .build();

        } catch (Exception e) {
            log.error("Login failed for customer email: {}", loginRequest.getEmail(), e);
            throw new BadRequestException("Invalid email or password");
        }
    }

    @Override
    public CustomerAuthResponse register(CustomerRegisterRequest registerRequest) {
        if (customerRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        Customer customer = new Customer();
        customer.setFirstName(registerRequest.getFirstName());
        customer.setLastName(registerRequest.getLastName());
        customer.setEmail(registerRequest.getEmail());
        customer.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        customer.setPhone(registerRequest.getPhone());
        customer.setDateOfBirth(registerRequest.getDateOfBirth());
        customer.setGender(registerRequest.getGender());
        customer.setIsActive(true);
        customer.setIsEmailVerified(false);

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        customer.setEmailVerificationToken(verificationToken);
        customer.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hours expiry

        Customer savedCustomer = customerRepository.save(customer);

        // For now, we'll automatically verify email for demo purposes
        // In a real application, you would send an email with the verification token
        log.info("Email verification token generated for {}: {}", savedCustomer.getEmail(), verificationToken);

        String token = jwtUtil.generateToken(savedCustomer.getEmail(), "CUSTOMER");

        log.info("Customer {} registered successfully", savedCustomer.getEmail());

        return CustomerAuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedCustomer.getId())
                .email(savedCustomer.getEmail())
                .firstName(savedCustomer.getFirstName())
                .lastName(savedCustomer.getLastName())
                .loyaltyPoints(savedCustomer.getLoyaltyPoints())
                .isEmailVerified(savedCustomer.getIsEmailVerified())
                .role("CUSTOMER")
                .build();
    }

    @Override
    public void logout(String token) {
        SecurityContextHolder.clearContext();
        log.info("Customer logged out successfully");
    }

    @Override
    public void verifyEmail(String token) {
        Customer customer = customerRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (customer.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        customer.setIsEmailVerified(true);
        customer.setEmailVerificationToken(null);
        customer.setEmailVerificationExpiresAt(null);

        customerRepository.save(customer);

        log.info("Email verified successfully for customer: {}", customer.getEmail());
    }

    @Override
    public void resendVerificationEmail(String email) {
        Customer customer = customerRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getIsEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        customer.setEmailVerificationToken(verificationToken);
        customer.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        customerRepository.save(customer);

        // In a real application, send email here
        log.info("New email verification token generated for {}: {}", customer.getEmail(), verificationToken);
    }
}