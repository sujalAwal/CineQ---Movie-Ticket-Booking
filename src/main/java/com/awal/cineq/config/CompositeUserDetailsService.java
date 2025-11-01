package com.awal.cineq.config;

import com.awal.cineq.customer.service.CustomerDetailsService;
import com.awal.cineq.user.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompositeUserDetailsService implements UserDetailsService {

    private final CustomUserDetailsService userDetailsService;
    private final CustomerDetailsService customerDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            // First try to load as a user
            return userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            try {
                // If not found as user, try to load as customer
                return customerDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException ex) {
                log.error("User/Customer not found with email: {}", username);
                throw new UsernameNotFoundException("User not found with email: " + username);
            }
        }
    }
}