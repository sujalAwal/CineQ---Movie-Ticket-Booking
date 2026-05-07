package com.awal.cineq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtAuthTokenFilter jwtAuthTokenFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz


                    // ============================================
                    // PUBLIC API
                    // ============================================
                    .requestMatchers("/public/**").permitAll()

                    // ============================================
                    // CUSTOMER AUTHENTICATION (Public exceptions)
                    // ============================================
                    .requestMatchers(
                            "/customer/auth/login",
                            "/customer/auth/login/google",
                            "/customer/auth/register",
                            "/customer/auth/verify-email",
                            "/customer/auth/forgot-password",
                            "/customer/auth/reset-password"
                    ).permitAll()

                    // ============================================
                    // CUSTOMER PROTECTED API
                    // ============================================
                    .requestMatchers("/customer/**").hasRole("CUSTOMER")

                    // ============================================
                    // HEALTH & DOCS
                    // ============================================
                    .requestMatchers("/health").permitAll()
//                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
//                    .requestMatchers("/h2-console/**").permitAll(

                     // ============================================
                    // ADMIN AUTHENTICATION
                    // ============================================
                    .requestMatchers(
                            "/auth/login",
                            "/auth/register",
                            "/auth/validate-token",
                            "/auth/set-password",
                            "/auth/resend-link"
                    ).permitAll()

                    // ============================================
                    // ADMIN API (Everything else - your clean way!)
                    // ============================================
                    .requestMatchers("/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "USER")
                
                // All other requests need authentication
                .anyRequest().authenticated()
            );

        // Add rate limiting filter (before JWT to prevent auth token consumption on rate limited requests)
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Add JWT filter
        http.addFilterBefore(jwtAuthTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // Security headers
        http.headers(headers -> headers
            .frameOptions(frame -> frame.sameOrigin())  // X-Frame-Options: SAMEORIGIN
            .xssProtection(xss -> xss.disable())  // Disable XSS protection (modern browsers use CSP)
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:;"
            ))
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)  // 1 year
                .includeSubDomains(true)
            )
            .contentTypeOptions(Customizer.withDefaults())  // X-Content-Type-Options: nosniff
            .referrerPolicy(referrer -> referrer.policy(
                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
            ))
        );

        return http.build();
    }
}
