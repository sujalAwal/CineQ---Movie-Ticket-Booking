package com.awal.cineq.user.service;

import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndIsActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getIsActive(),
                true,
                true,
                true,
                getAuthorities(user)
        );
    }

    /**
     * Fetches the role name from the roles collection using the user's roleId
     * and returns the appropriate authorities.
     *
     * @param user The User entity
     * @return Collection of granted authorities based on the user's role
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        String roleName = fetchRoleNameById(user.getRoleId());
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    /**
     * Fetches the role name from the roles collection by roleId.
     *
     * @param roleId The ObjectId of the role document
     * @return The role name (e.g., "ADMIN", "USER")
     */
    private String fetchRoleNameById(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            log.warn("fetchRoleNameById: roleId is null or blank, returning default role 'USER'");
            return "USER";
        }

        try {
            Query roleQuery = Query.query(
                    Criteria.where("_id").is(new org.bson.types.ObjectId(roleId))
                            .and("deletedAt").is(null)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> roleDoc = mongoTemplate.findOne(roleQuery, Map.class, "roles");

            if (roleDoc != null && roleDoc.get("name") != null) {
                String roleName = roleDoc.get("name").toString();
                log.debug("fetchRoleNameById: Found role - id={}, name={}", roleId, roleName);
                return roleName;
            } else {
                log.warn("fetchRoleNameById: Role not found for roleId={}, returning default 'USER'", roleId);
                return "USER";
            }
        } catch (IllegalArgumentException e) {
            log.warn("fetchRoleNameById: Invalid ObjectId format for roleId={}, returning default 'USER'", roleId);
            return "USER";
        }
    }
}