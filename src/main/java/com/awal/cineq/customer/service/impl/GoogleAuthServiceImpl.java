package com.awal.cineq.customer.service.impl;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.customer.dto.CustomerAuthResponse;
import com.awal.cineq.customer.dto.GoogleLoginRequest;
import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import com.awal.cineq.customer.service.GoogleAuthService;
import com.awal.cineq.exception.BadRequestException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final ApplicationProperties appProperties;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;

    @Override
    public CustomerAuthResponse login(GoogleLoginRequest googleLoginRequest, HttpServletResponse response) {
        String googleClientId = appProperties.getApp().getGoogleClientId();
        if (googleClientId == null || googleClientId.isBlank()) {
            log.error("Google Client ID is not configured. Check app.app.google-client-id property.");
            throw new BadRequestException("Google login is not configured");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleLoginRequest.getCredential());

            log.debug("Google Id token: {}", idToken);
            if (idToken == null) {
                throw new BadRequestException("Invalid token");
            }

            Payload payload = idToken.getPayload();
            log.debug("Google Id token payload: {}", payload);

            String email = payload.getEmail().toLowerCase().trim();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            Boolean emailVerified = payload.getEmailVerified();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BadRequestException("Google email is not verified");
            }

            // Find existing customer or create new one
            Optional<Customer> existingCustomer = customerRepository.findByEmailAndIsActiveTrue(email);

            Customer customer;
            if (existingCustomer.isPresent()) {
                customer = existingCustomer.get();
                log.info("Existing customer logged in via Google: {}", email);
            } else {
                customer = new Customer();
                customer.setEmail(email);
                customer.setFirstName(firstName);
                customer.setLastName(lastName);
                customer.setIsEmailVerified(emailVerified);
                customer.setIsActive(true);
                customer.setLoyaltyPoints(0);
                customer.setFailedLoginAttempts(0);
                customer = customerRepository.save(customer);
                log.info("New customer created via Google login: {}", email);
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(customer.getEmail(), "CUSTOMER");

            // Set HttpOnly cookie
            Cookie authCookie = new Cookie(appProperties.getSecurity().getCookie().getName(), token);
            authCookie.setHttpOnly(appProperties.getSecurity().getCookie().isHttpOnly());
            authCookie.setSecure(appProperties.getSecurity().getCookie().isSecure());
            authCookie.setPath(appProperties.getSecurity().getCookie().getPath());
            authCookie.setMaxAge(appProperties.getSecurity().getCookie().getMaxAge());
            authCookie.setAttribute("SameSite", appProperties.getSecurity().getCookie().getSameSite());
            response.addCookie(authCookie);

            // Return response matching existing login format (token NOT in body)
            return CustomerAuthResponse.builder()
                    .token(null)
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

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new BadRequestException("Google authentication failed");
        }
    }
}
