package com.awal.cineq.customer.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.awal.cineq.common.util.EmailHelper;
import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.customer.config.CustomerAuthConfig;
import com.awal.cineq.customer.dto.CustomerAuthResponse;
import com.awal.cineq.customer.dto.CustomerLoginRequest;
import com.awal.cineq.customer.dto.CustomerRegisterRequest;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.customer.service.CustomerAuthService;
import com.awal.cineq.email.model.EmailTemplate;
import com.awal.cineq.email.repository.EmailTemplateRepository;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerAuthServiceImpl implements CustomerAuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomerAuthConfig customerAuthConfig;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailHelper emailHelper;

    @Override
    public CustomerAuthResponse login(CustomerLoginRequest loginRequest) {
        // Find customer by email
        Customer customer = customerRepository.findByEmailAndIsActiveTrue(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Check if account is locked
        if (customer.isLocked()) {
            long minutesRemaining = Duration.between(LocalDateTime.now(), customer.getLockedUntil()).toMinutes() + 1;
            throw new BadRequestException("Account locked due to too many failed attempts. Try again in " + minutesRemaining + " minutes.");
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), customer.getPassword())) {
            handleFailedLogin(customer);
            int attemptsRemaining = customerAuthConfig.getMaxFailedLoginAttempts() - customer.getFailedLoginAttempts();
            if (attemptsRemaining > 0) {
                throw new BadRequestException("Invalid email or password. " + attemptsRemaining + " attempts remaining before lockout.");
            } else {
                throw new BadRequestException("Account locked due to too many failed attempts. Try again in " + customerAuthConfig.getLockoutDurationMinutes() + " minutes.");
            }
        }

        // Check email verification requirement
        if (customerAuthConfig.isEmailVerificationRequired() && !customer.getIsEmailVerified()) {
            throw new BadRequestException("Please verify your email before logging in. Check your inbox or request a new verification email.");
        }

        // Successful login - reset failed attempts
        customer.resetFailedLoginAttempts();
        customerRepository.save(customer);

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
    }

    private void handleFailedLogin(Customer customer) {
        customer.incrementFailedLoginAttempts();
        
        if (customer.getFailedLoginAttempts() >= customerAuthConfig.getMaxFailedLoginAttempts()) {
            customer.lockAccount(customerAuthConfig.getLockoutDurationMinutes());
            log.warn("Customer {} account locked after {} failed attempts", 
                    customer.getEmail(), customer.getFailedLoginAttempts());
        }
        
        customerRepository.save(customer);
        log.info("Failed login attempt for customer {}. Attempts: {}/{}", 
                customer.getEmail(), customer.getFailedLoginAttempts(), customerAuthConfig.getMaxFailedLoginAttempts());
    }

    @Override
    public CustomerAuthResponse register(CustomerRegisterRequest registerRequest) {

        // Validate password confirmation matches
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

         if (customerRepository.existsByEmail(registerRequest.getEmail())) {
           // throw new DuplicateResourceException("Email already exists");
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
        customer.setFailedLoginAttempts(0);

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        customer.setEmailVerificationToken(verificationToken);
        customer.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        Customer savedCustomer = customerRepository.save(customer);

        // Send verification email
        sendVerificationEmail(savedCustomer, verificationToken);
        log.info("Email verification token generated for {}: {}", savedCustomer.getEmail(), verificationToken);

        // If verification required, don't return token
        if (customerAuthConfig.isEmailVerificationRequired()) {
            log.info("Customer {} registered - email verification required", savedCustomer.getEmail());
            return CustomerAuthResponse.builder()
                    .id(savedCustomer.getId())
                    .email(savedCustomer.getEmail())
                    .firstName(savedCustomer.getFirstName())
                    .lastName(savedCustomer.getLastName())
                    .loyaltyPoints(savedCustomer.getLoyaltyPoints())
                    .isEmailVerified(false)
                    .role("CUSTOMER")
                    .token(null)
                    .type(null)
                    .build();
        }

        // If verification not required, return token immediately
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
            throw new BadRequestException("Verification token has expired. Please request a new one.");
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

        // Send verification email
        sendVerificationEmail(customer, verificationToken);
        log.info("New email verification token generated for {}: {}", customer.getEmail(), verificationToken);
    }

    @Override
    public void forgotPassword(String email) {
        Customer customer = customerRepository.findByEmailAndIsActiveTrue(email)
                .orElse(null);
        
        // Always return success to prevent email enumeration attacks
        if (customer == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        // Generate password reset token
        String resetToken = UUID.randomUUID().toString();
        customer.setPasswordResetToken(resetToken);
        customer.setPasswordResetTokenExpiresAt(
                LocalDateTime.now().plusHours(customerAuthConfig.getPasswordResetTokenExpiryHours())
        );

        customerRepository.save(customer);

        // TODO: Send actual password reset email with link
        log.info("Password reset token generated for {}: {}", customer.getEmail(), resetToken);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        Customer customer = customerRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (customer.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        // Update password
        customer.setPassword(passwordEncoder.encode(newPassword));
        
        // Clear reset token
        customer.setPasswordResetToken(null);
        customer.setPasswordResetTokenExpiresAt(null);
        
        // Clear any lockout (password reset should unlock account)
        customer.resetFailedLoginAttempts();

        customerRepository.save(customer);
        log.info("Password reset successfully for customer: {}", customer.getEmail());
    }

    @Override
    public boolean validateResetToken(String token) {
        return customerRepository.findByPasswordResetToken(token)
                .map(customer -> customer.getPasswordResetTokenExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    // UserDetailsService method for Spring Security
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) {
        var byEmail = customerRepository.findByEmailAndIsActiveTrue(username);
        if (byEmail.isPresent()) {
            Customer c = byEmail.get();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(c.getEmail())
                    .password(c.getPassword())
                    .roles("CUSTOMER")
                    .build();
        }

        try {
            var byId = customerRepository.findById(username);
            if (byId.isPresent()) {
                Customer c = byId.get();
                return org.springframework.security.core.userdetails.User.builder()
                        .username(c.getEmail())
                        .password(c.getPassword())
                        .roles("CUSTOMER")
                        .build();
            }
            throw new UsernameNotFoundException("Customer not found: " + username);
        } catch (IllegalArgumentException ex) {
            throw new UsernameNotFoundException("Invalid identifier or user not found: " + username, ex);
        }
    }

    private void sendVerificationEmail(Customer customer, String verificationToken) {
        try {
            EmailTemplate template = emailTemplateRepository
                    .findBySlugAndIsActiveTrueAndDeletedAtNull("email-verification")
                    .orElse(null);

            if (template == null) {
                log.warn("Email verification template not found. Email will not be sent.");
                return;
            }

            // Build verification link using configured frontend URL
            // Frontend page (/verify-email) should:
            // 1. Extract token from URL params
            // 2. Call POST /api/frontend/customer/auth/verify-email?token=XXX
            // 3. Show success/error to user
            String frontendUrl = customerAuthConfig.getFrontendUrl();
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

            // Replace placeholders in subject and message
            String subject = template.getSubject()
                    .replace("{{firstName}}", customer.getFirstName())
                    .replace("{{year}}", String.valueOf(Year.now().getValue()));

            String message = template.getMessage()
                    .replace("{{firstName}}", customer.getFirstName())
                    .replace("{{verificationLink}}", verificationLink)
                    .replace("{{year}}", String.valueOf(Year.now().getValue()));

            // Send email
            boolean sent = emailHelper.sendEmail(customer.getEmail(), subject, message);
            if (sent) {
                log.info("Verification email sent successfully to {}", customer.getEmail());
            } else {
                log.warn("Failed to send verification email to {}", customer.getEmail());
            }
        } catch (Exception e) {
            log.error("Error sending verification email to {}: {}", customer.getEmail(), e.getMessage(), e);
        }
    }
}
