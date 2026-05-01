package com.awal.cineq.customer.service.impl;

import com.awal.cineq.customer.model.Customer;
import com.awal.cineq.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service("customerUserDetailsService")
@RequiredArgsConstructor
public class CustomerDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found with email: " + email));

        // Google OAuth customers have null password - use placeholder since they authenticate via OAuth
        String password = customer.getPassword() != null ? customer.getPassword() : "";

        return new org.springframework.security.core.userdetails.User(
                customer.getEmail(),
                password,
                customer.getIsActive(),
                true,
                true,
                true,
                getAuthorities()
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}