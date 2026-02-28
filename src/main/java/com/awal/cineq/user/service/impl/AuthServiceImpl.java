package com.awal.cineq.user.service.impl;

import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.user.dto.AuthResponse;
import com.awal.cineq.user.service.AuthService;
import com.awal.cineq.user.dto.LoginRequest;
import com.awal.cineq.user.dto.ProfileResponse;
import com.awal.cineq.user.dto.RegisterRequest;
import com.awal.cineq.user.dto.UserDTO;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("AuthServiceImpl: Starting login process for email: {}", loginRequest.getEmail());

        try {
            // Validate input
            if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()) {
                log.warn("Login attempt with invalid email");
                throw new BadRequestException("Email is required");
            }

            // Find user by email
            User user = userRepository.findByEmailAndIsActiveTrue(loginRequest.getEmail())
                    .orElseThrow(() -> {
                        log.debug("User not found for email: {}", loginRequest.getEmail());
                        return new BadRequestException("Invalid email or password");
                    });

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.debug("Password mismatch for user: {}", loginRequest.getEmail());
                throw new BadRequestException("Invalid email or password");
            }

            // Fetch role name from roles collection using roleId
            String roleName = fetchRoleNameById(user.getRoleId());

            // Generate JWT token with role name
            String token = jwtUtil.generateToken(user.getEmail(), roleName);
            log.info("User {} logged in successfully", user.getEmail());

            // Map user to DTO and set role info
            UserDTO userDTO = mapUserToDTO(user, roleName);

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(userDTO)
                    .build();

        } catch (BadRequestException e) {
            log.debug("Login validation failed for email: {}", loginRequest.getEmail());
            throw e;
        } catch (ExceptionInInitializerError e) {
            log.error("ExceptionInInitializerError during login for email: {}. Root cause: ", loginRequest.getEmail(), e);
            log.error("Caused by: ", e.getCause());
            throw new BadRequestException("Login service initialization failed. Please try again later.");
        } catch (Exception e) {
            log.error("Unexpected error during login for email: {}", loginRequest.getEmail(), e);
            throw new BadRequestException("Login failed. Please try again later.");
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("AuthServiceImpl: Starting registration for email: {}", registerRequest.getEmail());

        if (registerRequest.getPassword() == null || registerRequest.getPassword().length() < 6) {
            log.warn("Registration attempt with weak password for email: {}", registerRequest.getEmail());
            throw new BadRequestException("Password must be at least 6 characters long");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration attempt with duplicate email: {}", registerRequest.getEmail());
            throw new DuplicateResourceException("Email already exists");
        }

        // Resolve roleId: use provided roleId or find default role
        String roleId = registerRequest.getRoleId();
        if (roleId == null || roleId.isBlank()) {
            roleId = findDefaultRoleId();
        } else {
            // Validate that the provided roleId exists
            validateRoleExists(roleId);
        }

        // Create user
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRoleId(roleId);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        // Fetch role name for JWT token generation
        String roleName = fetchRoleNameById(savedUser.getRoleId());

        // Generate JWT token with role name
        String token = jwtUtil.generateToken(savedUser.getEmail(), roleName);

        log.info("User {} registered successfully", savedUser.getEmail());

        // Map user to DTO and set role info
        UserDTO userDTO = mapUserToDTO(savedUser, roleName);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(userDTO)
                .build();
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

    /**
     * Finds the default role ID (USER role) from the roles collection.
     *
     * @return The ObjectId of the default role
     */
    private String findDefaultRoleId() {
        Query roleQuery = Query.query(
                Criteria.where("name").is("USER")
                        .and("deletedAt").is(null)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> roleDoc = mongoTemplate.findOne(roleQuery, Map.class, "roles");

        if (roleDoc != null && roleDoc.get("_id") != null) {
            String roleId = roleDoc.get("_id").toString();
            log.debug("findDefaultRoleId: Found default USER role - id={}", roleId);
            return roleId;
        }

        log.error("findDefaultRoleId: Default USER role not found in roles collection");
        throw new ResourceNotFoundException("Default USER role not found. Please ensure roles are properly configured.");
    }

    /**
     * Validates that a role with the given ID exists in the roles collection.
     *
     * @param roleId The ObjectId of the role document to validate
     * @throws ResourceNotFoundException if the role is not found
     */
    private void validateRoleExists(String roleId) {
        try {
            Query roleQuery = Query.query(
                    Criteria.where("_id").is(new org.bson.types.ObjectId(roleId))
                            .and("deletedAt").is(null)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> roleDoc = mongoTemplate.findOne(roleQuery, Map.class, "roles");

            if (roleDoc == null) {
                log.warn("validateRoleExists: Role not found for roleId={}", roleId);
                throw new ResourceNotFoundException("Role not found with ID: " + roleId);
            }

            log.debug("validateRoleExists: Role validated - id={}", roleId);
        } catch (IllegalArgumentException e) {
            log.warn("validateRoleExists: Invalid ObjectId format for roleId={}", roleId);
            throw new BadRequestException("Invalid role ID format: " + roleId);
        }
    }

    /**
     * Maps a User entity to UserDTO, including role information.
     *
     * @param user     The User entity
     * @param roleName The role name fetched from roles collection
     * @return The mapped UserDTO
     */
    private UserDTO mapUserToDTO(User user, String roleName) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRoleId(user.getRoleId());
        dto.setRoleName(roleName);
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    @Override
    public void logout(String token) {
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        log.info("getProfile STARTED: email={}", email);

        try {
            // Step 1: Get user from users collection
            User user = userRepository.findByEmailAndIsActiveTrue(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            log.debug("getProfile: Found user - id={}, name={}, roleId={}",
                    user.getId(), user.getName(), user.getRoleId());

            // Step 2: Get role details from roles collection using roleId
            String roleId = user.getRoleId();
            String roleName = "USER"; // Default
            ProfileResponse.RoleInfo roleInfo = null;

            if (roleId != null && !roleId.isBlank()) {
                try {
                    Query roleQuery = Query.query(
                            Criteria.where("_id").is(new org.bson.types.ObjectId(roleId))
                                    .and("deletedAt").is(null)
                    );

                    @SuppressWarnings("unchecked")
                    Map<String, Object> roleDoc = mongoTemplate.findOne(roleQuery, Map.class, "roles");

                    if (roleDoc != null) {
                        roleName = roleDoc.get("name") != null ? roleDoc.get("name").toString() : "USER";

                        @SuppressWarnings("unchecked")
                        Map<String, Object> permissions = (Map<String, Object>) roleDoc.get("permissions");

                        roleInfo = ProfileResponse.RoleInfo.builder()
                                .id(roleId)
                                .name(roleName)
                                .slug((String) roleDoc.get("slug"))
                                .permissions(permissions)
                                .isActive(roleDoc.get("isActive") != null ? (Boolean) roleDoc.get("isActive") : true)
                                .createdAt(parseDateTime(roleDoc.get("createdAt")))
                                .updatedAt(parseDateTime(roleDoc.get("updatedAt")))
                                .build();

                        log.debug("getProfile: Found role - id={}, name={}", roleId, roleName);
                    } else {
                        log.warn("getProfile: Role not found in roles collection for roleId: {}", roleId);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("getProfile: Invalid ObjectId format for roleId: {}", roleId);
                }
            } else {
                log.warn("getProfile: User has no roleId assigned");
            }

            // Step 3: Get modules from role_has_modules collection
            List<ProfileResponse.ModuleInfo> modules = new ArrayList<>();

            if (roleId != null) {
                Query roleHasModulesQuery = Query.query(
                        Criteria.where("roleId").is(roleId)
                                .and("deletedAt").is(null)
                );

                List<?> roleHasModulesRaw = mongoTemplate.find(
                        roleHasModulesQuery, Map.class, "role_has_modules");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> roleHasModules = (List<Map<String, Object>>) roleHasModulesRaw;

                log.debug("getProfile: Found {} role_has_modules entries", roleHasModules.size());

                for (Map<String, Object> rhm : roleHasModules) {
                    String moduleId = (String) rhm.get("moduleId");

                    if (moduleId == null) continue;

                    // Get full module details from modules collection
                    Query moduleQuery = Query.query(
                            Criteria.where("_id").is(moduleId)
                                    .and("deletedAt").is(null)
                    );

                    @SuppressWarnings("unchecked")
                    Map<String, Object> moduleDoc = mongoTemplate.findOne(moduleQuery, Map.class, "modules");

                    if (moduleDoc != null) {
                        @SuppressWarnings("unchecked")
                        List<Object> permissionIds = (List<Object>) rhm.get("permissionIds");

                        ProfileResponse.ModuleInfo moduleInfo = ProfileResponse.ModuleInfo.builder()
                                .id(moduleDoc.get("_id").toString())
                                .code(moduleDoc.get("code") instanceof Integer
                                        ? (Integer) moduleDoc.get("code")
                                        : Integer.parseInt(moduleDoc.get("code").toString()))
                                .name((String) moduleDoc.get("name"))
                                .displayName((String) moduleDoc.get("display_name"))
                                .api((String) moduleDoc.get("api"))
                                .description((String) moduleDoc.get("description"))
                                .isEnabled(moduleDoc.get("is_enabled") != null
                                        ? (Boolean) moduleDoc.get("is_enabled")
                                        : true)
                                .permissionIds(permissionIds)
                                .build();

                        modules.add(moduleInfo);
                        log.debug("getProfile: Added module - code={}, name={}",
                                moduleInfo.getCode(), moduleInfo.getName());
                    }
                }
            }

            // Step 4: Build and return ProfileResponse
            ProfileResponse response = ProfileResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .roleName(roleName)
                    .isActive(user.getIsActive())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .role(roleInfo)
                    .modules(modules)
                    .build();

            log.info("getProfile END: email={}, moduleCount={}", email, modules.size());
            return response;

        } catch (ResourceNotFoundException e) {
            log.error("getProfile: User not found - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getProfile ERROR: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to fetch profile");
        }
    }

    /**
     * Helper to parse LocalDateTime from various MongoDB date formats
     */
    private LocalDateTime parseDateTime(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDateTime) return (LocalDateTime) dateObj;
        if (dateObj instanceof java.util.Date) {
            return ((java.util.Date) dateObj).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        return null;
    }


}
