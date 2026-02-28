package com.awal.cineq.customer.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.awal.cineq.common.util.EmailHelper;
import com.awal.cineq.common.util.SecureTokenGenerator;
import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.customer.dto.CustomerAuthResponse;
import com.awal.cineq.customer.dto.CustomerLoginRequest;
import com.awal.cineq.customer.dto.CustomerRegisterRequest;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.customer.service.CustomerAuthService;
import com.awal.cineq.customer.service.PasswordHistoryService;
import com.awal.cineq.customer.service.TokenBlacklistService;
import com.awal.cineq.email.model.EmailTemplate;
import com.awal.cineq.email.repository.EmailTemplateRepository;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.ResourceNotFoundException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final ApplicationProperties appProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordHistoryService passwordHistoryService;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailHelper emailHelper;

    @Override
    public CustomerAuthResponse login(CustomerLoginRequest loginRequest, HttpServletResponse response) {
        // Normalize email to lowercase
        String normalizedEmail = normalizeEmail(loginRequest.getEmail());
        
        // Find customer by email
        Customer customer = customerRepository.findByEmailAndIsActiveTrue(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        // Check if account is locked
        if (customer.isLocked()) {
            long minutesRemaining = Duration.between(LocalDateTime.now(), customer.getLockedUntil()).toMinutes() + 1;
            throw new BadRequestException("Account locked due to too many failed attempts. Try again in " + minutesRemaining + " minutes.");
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), customer.getPassword())) {
            handleFailedLogin(customer);
            // Check if account is now locked after failed attempt
            if (customer.isLocked()) {
                long minutesRemaining = Duration.between(LocalDateTime.now(), customer.getLockedUntil()).toMinutes() + 1;
                throw new BadRequestException("Account temporarily locked. Please try again in " + minutesRemaining + " minutes.");
            }
            // Generic error message to prevent email enumeration
            throw new BadRequestException("Invalid credentials");
        }

        // Check email verification requirement
        if (appProperties.getCustomer().isEmailVerificationRequired() && !customer.getIsEmailVerified()) {
            throw new BadRequestException("Please verify your email before logging in. Check your inbox or request a new verification email.");
        }

        // Successful login - reset failed attempts
        customer.resetFailedLoginAttempts();
        customerRepository.save(customer);

        String token = jwtUtil.generateToken(customer.getEmail(), "CUSTOMER");
        
        // Set HttpOnly cookie for secure token storage
        Cookie authCookie = new Cookie(appProperties.getSecurity().getCookie().getName(), token);
        authCookie.setHttpOnly(appProperties.getSecurity().getCookie().isHttpOnly());
        authCookie.setSecure(appProperties.getSecurity().getCookie().isSecure());
        authCookie.setPath(appProperties.getSecurity().getCookie().getPath());
        authCookie.setMaxAge(appProperties.getSecurity().getCookie().getMaxAge());
        authCookie.setAttribute("SameSite", appProperties.getSecurity().getCookie().getSameSite());
        
        response.addCookie(authCookie);
        
        log.info("Customer {} logged in successfully", customer.getEmail());

        // Return response WITHOUT token in body (token is in HttpOnly cookie)
        return CustomerAuthResponse.builder()
                .token(null)  // Don't send token to client - cookie handles auth
                .type(null)
                .id(customer.getId())
                .email(customer.getEmail())
                .firstName(customer.getFirstName())
                .middleName(customer.getMiddleName())
                .lastName(customer.getLastName())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .isEmailVerified(customer.getIsEmailVerified())
                .role("CUSTOMER")
                .build();
    }

    private void handleFailedLogin(Customer customer) {
        customer.incrementFailedLoginAttempts();
        
        int attempts = customer.getFailedLoginAttempts();
        
        // Progressive lockout durations (exponential backoff)
        if (attempts >= 10) {
            customer.lockAccount(60);  // 1 hour after 10+ attempts
            log.warn("Customer {} account locked for 60 minutes after {} failed attempts", 
                    customer.getEmail(), attempts);
        } else if (attempts >= 7) {
            customer.lockAccount(30);  // 30 minutes after 7-9 attempts
            log.warn("Customer {} account locked for 30 minutes after {} failed attempts", 
                    customer.getEmail(), attempts);
        } else if (attempts >= 5) {
            customer.lockAccount(15);  // 15 minutes after 5-6 attempts
            log.warn("Customer {} account locked for 15 minutes after {} failed attempts", 
                    customer.getEmail(), attempts);
        } else if (attempts >= 3) {
            customer.lockAccount(5);   // 5 minutes after 3-4 attempts
            log.warn("Customer {} account locked for 5 minutes after {} failed attempts", 
                    customer.getEmail(), attempts);
        } else {
            // Less than 3 attempts - just log, no lockout
            log.info("Failed login attempt {} for customer {}", attempts, customer.getEmail());
        }
        
        customerRepository.save(customer);
    }

    @Override
    public CustomerAuthResponse register(CustomerRegisterRequest registerRequest) {

        // Validate password confirmation matches
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Normalize email to lowercase
        String normalizedEmail = normalizeEmail(registerRequest.getEmail());
        
        // Check for duplicate (only active customers) - generic error message to prevent email enumeration
        if (customerRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new BadRequestException("Unable to complete registration. Please try again or contact support.");
        }

        Customer customer = new Customer();
        customer.setFirstName(registerRequest.getFirstName());
        customer.setMiddleName(registerRequest.getMiddleName());
        customer.setLastName(registerRequest.getLastName());
        customer.setEmail(normalizedEmail);
        customer.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        customer.setPhone(registerRequest.getPhone());
        customer.setDateOfBirth(registerRequest.getDateOfBirth());
        customer.setGender(registerRequest.getGender());
        customer.setIsActive(true);
        customer.setIsEmailVerified(false);
        customer.setFailedLoginAttempts(0);

        // Generate email verification token (cryptographically secure)
        String verificationToken = SecureTokenGenerator.generateToken();
        customer.setEmailVerificationToken(verificationToken);
        customer.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        Customer savedCustomer = customerRepository.save(customer);

        // Send verification email
        sendVerificationEmail(savedCustomer, verificationToken);
        log.info("Email verification initiated for customer: {}", savedCustomer.getEmail());

        // If verification required, don't return token
        if (appProperties.getCustomer().isEmailVerificationRequired()) {
            log.info("Customer {} registered - email verification required", savedCustomer.getEmail());
            return CustomerAuthResponse.builder()
                    .id(savedCustomer.getId())
                    .email(savedCustomer.getEmail())
                    .firstName(savedCustomer.getFirstName())
                    .middleName(savedCustomer.getMiddleName())
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
                .middleName(savedCustomer.getMiddleName())
                .lastName(savedCustomer.getLastName())
                .loyaltyPoints(savedCustomer.getLoyaltyPoints())
                .isEmailVerified(savedCustomer.getIsEmailVerified())
                .role("CUSTOMER")
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // Extract token from cookie before clearing it
        String token = extractTokenFromRequest(request);
        
        // Blacklist the token to prevent reuse
        if (token != null && !token.isEmpty()) {
            tokenBlacklistService.blacklistToken(token);
            log.info("Token blacklisted successfully");
        }
        
        // Clear the HttpOnly cookie
        Cookie authCookie = new Cookie(appProperties.getSecurity().getCookie().getName(), null);
        authCookie.setHttpOnly(appProperties.getSecurity().getCookie().isHttpOnly());
        authCookie.setSecure(appProperties.getSecurity().getCookie().isSecure());
        authCookie.setPath(appProperties.getSecurity().getCookie().getPath());
        authCookie.setMaxAge(0);  // Expire immediately
        
        response.addCookie(authCookie);
        SecurityContextHolder.clearContext();
        log.info("Customer logged out successfully");
    }
    
    /**
     * Extract JWT token from request (Authorization header or cookie)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Try Authorization header first
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        
        // Then try cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (appProperties.getSecurity().getCookie().getName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
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
        String normalizedEmail = normalizeEmail(email);
        Customer customer = customerRepository.findByEmailAndIsActiveTrue(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (customer.getIsEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        // Generate new verification token (cryptographically secure)
        String verificationToken = SecureTokenGenerator.generateToken();
        customer.setEmailVerificationToken(verificationToken);
        customer.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        customerRepository.save(customer);

        // Send verification email
        sendVerificationEmail(customer, verificationToken);
        log.info("Verification email resent to customer: {}", customer.getEmail());
    }

    @Override
    public void forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);
        Customer customer = customerRepository.findByEmailAndIsActiveTrue(normalizedEmail)
                .orElse(null);
        
        // Always return success to prevent email enumeration attacks
        if (customer == null) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        // Generate password reset token (cryptographically secure)
        String resetToken = SecureTokenGenerator.generateToken();
        customer.setPasswordResetToken(resetToken);
        customer.setPasswordResetTokenExpiresAt(
                LocalDateTime.now().plusHours(appProperties.getCustomer().getPasswordResetTokenExpiryHours())
        );

        customerRepository.save(customer);

        // TODO: Send actual password reset email with link
        log.info("Password reset initiated for customer: {}", customer.getEmail());
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        Customer customer = customerRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (customer.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        // Validate password not in recent history
        passwordHistoryService.validatePasswordNotReused(customer.getId(), newPassword);

        // Encode new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        
        // Add current password to history before updating
        if (customer.getPassword() != null && !customer.getPassword().isEmpty()) {
            passwordHistoryService.addPasswordToHistory(customer.getId(), customer.getPassword());
        }
        
        // Update password
        customer.setPassword(encodedPassword);
        
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
            String frontendUrl = appProperties.getCustomer().getFrontendUrl();
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
    
    /**
     * Normalize email address to lowercase and trim whitespace
     * @param email Raw email address
     * @return Normalized email address
     */
    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.toLowerCase().trim();
    }
}
