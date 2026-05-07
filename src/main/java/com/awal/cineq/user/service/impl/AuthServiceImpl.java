package com.awal.cineq.user.service.impl;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.config.JwtUtil;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.form.enums.FormAction;
import com.awal.cineq.user.dto.AuthResponse;
import com.awal.cineq.user.dto.LoginRequest;
import com.awal.cineq.user.dto.ProfileResponse;
import com.awal.cineq.user.dto.RegisterRequest;
import com.awal.cineq.user.dto.UserDTO;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.model.UserHasRole;
import com.awal.cineq.user.repository.UserRepository;
import com.awal.cineq.user.repository.UserHasRoleRepository;
import com.awal.cineq.user.service.AuthService;
import com.awal.cineq.user.service.UserRoleAssigner;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import com.awal.cineq.common.util.EmailHelper;
import com.awal.cineq.common.util.SecureTokenGenerator;
import com.awal.cineq.email.model.EmailTemplate;
import com.awal.cineq.email.repository.EmailTemplateRepository;
import com.awal.cineq.user.model.UserToken;
import com.awal.cineq.user.repository.UserTokenRepository;
import java.util.HashMap;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserHasRoleRepository userHasRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MongoTemplate mongoTemplate;
    private final ApplicationProperties applicationProperties;
    
    private final UserRoleAssigner userRoleAssigner;
    private final ApplicationProperties appProperties;
    private final UserTokenRepository userTokenRepository;
    private final EmailHelper emailHelper;
    private final EmailTemplateRepository emailTemplateRepository;


    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("AuthServiceImpl: Starting login process for email: {}", loginRequest.getEmail());

        try {
            if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getEmail().isBlank()) {
                log.warn("Login attempt with invalid email");
                throw new BadRequestException("Email is required");
            }

            User user = userRepository.findByEmailAndIsActiveTrue(loginRequest.getEmail())
                    .orElseThrow(() -> {
                        log.debug("User not found for email: {}", loginRequest.getEmail());
                        return new BadRequestException("Invalid email or password");
                    });

            if (user.getPassword() == null || user.getPassword().isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                log.debug("Password mismatch for user: {}", loginRequest.getEmail());
                throw new BadRequestException("Invalid email or password");
            }

            List<UserHasRole> activeRoles = userHasRoleRepository.findByUserIdActive(user.getId());
            LinkedHashMap<String, String> activeRoleNameById = resolveActiveRoleNamesById(activeRoles);
            List<String> roleIds = new ArrayList<>(activeRoleNameById.keySet());
            List<String> roleNames = new ArrayList<>(activeRoleNameById.values());
            String token = jwtUtil.generateToken(user.getEmail(), String.join(",", roleNames));
            log.info("User {} logged in successfully with roles: {}", user.getEmail(), roleNames);

            UserDTO userDTO = mapUserToDTO(user, roleIds, roleNames);

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

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration attempt with duplicate email: {}", registerRequest.getEmail());
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(null); // No default password
        user.setPasswordStatus("PENDING");
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setIsActive(false); // User is inactive

        User savedUser = userRepository.save(user);

        userRoleAssigner.replaceUserRoles(savedUser.getId(), registerRequest.getRoleIds());

        // Generate token and save UserToken document
        String tokenStr = SecureTokenGenerator.generateToken();
        UserToken userToken = new UserToken();
        userToken.setUserId(savedUser.getId());
        userToken.setToken(tokenStr);
        userToken.setCreatedAt(LocalDateTime.now());
        userToken.setExpiresAt(LocalDateTime.now().plusHours(appProperties.getToken().getExpiryHours()));
        userTokenRepository.save(userToken);

        // Build magic link
        String adminUrl = appProperties.getCustomer().getAdminUrl();
        String magicLink = adminUrl + "/set-password?token=" + tokenStr;

        // Send email if template exists
        emailTemplateRepository.findBySlugAndIsActiveTrueAndDeletedAtNull("password-setup").ifPresent(template -> {
            String msg = template.getMessage();
            if (msg != null) {
                msg = msg.replace("{{userName}}", savedUser.getName())
                         .replace("{{userEmail}}", savedUser.getEmail())
                         .replace("{{magicLink}}", magicLink)
                         .replace("{{expiryHours}}", String.valueOf(appProperties.getToken().getExpiryHours()))
                         .replace("{{year}}", String.valueOf(LocalDateTime.now().getYear()));
                emailHelper.sendEmail(savedUser.getEmail(), "Set up your CineQ Admin password", msg);
            }
            
            String adminMsg = template.getAdminMessage();
            if (adminMsg != null && !adminMsg.trim().isEmpty() && appProperties.getCustomer().isAdminNotificationEnabled()) {
                adminMsg = adminMsg.replace("{{userName}}", savedUser.getName())
                                   .replace("{{userEmail}}", savedUser.getEmail())
                                   .replace("{{timestamp}}", LocalDateTime.now().toString());
                emailHelper.sendEmail(
                    appProperties.getCustomer().getAdminEmailList(),
                    "🔐 New Staff User Registered: " + savedUser.getName(),
                    adminMsg
                );
            }
        });

        // The auth response is simplified since there is no JWT yet.
        return AuthResponse.builder()
                .user(mapUserToDTO(savedUser, new ArrayList<>(), new ArrayList<>()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> validateToken(String token) {
        UserToken userToken = userTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (!userToken.isValid()) {
            throw new BadRequestException("Token is invalid, expired, or has been used already");
        }

        User user = userRepository.findById(userToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("email", user.getEmail());
        return response;
    }

    @Override
    @Transactional
    public void setPassword(String token, String password) {
        UserToken userToken = userTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (!userToken.isValid()) {
            throw new BadRequestException("Token is invalid, expired, or has been used already");
        }

        User user = userRepository.findById(userToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordStatus("SET");
        userRepository.save(user);

        userToken.setUsedAt(LocalDateTime.now());
        userTokenRepository.save(userToken);

        // Send admin notification
        emailTemplateRepository.findBySlugAndIsActiveTrueAndDeletedAtNull("password-setup-complete").ifPresent(template -> {
            String msg = template.getMessage();
            if (msg != null && !msg.trim().isEmpty()) {
                String magicLink = appProperties.getCustomer().getAdminUrl() + "/login";
                msg = msg.replace("{{userName}}", user.getName())
                         .replace("{{userEmail}}", user.getEmail())
                         .replace("{{timestamp}}", LocalDateTime.now().toString())
                         .replace("{{magicLink}}", magicLink)
                         .replace("{{year}}", String.valueOf(LocalDateTime.now().getYear()));
                emailHelper.sendEmail(user.getEmail(), "Your CineQ Admin password is ready", msg);
            }

            String adminMsg = template.getAdminMessage();
            if (adminMsg != null && !adminMsg.trim().isEmpty() && appProperties.getCustomer().isAdminNotificationEnabled()) {
                adminMsg = adminMsg.replace("{{userName}}", user.getName())
                                   .replace("{{userEmail}}", user.getEmail())
                                   .replace("{{timestamp}}", LocalDateTime.now().toString());
                emailHelper.sendEmail(
                    appProperties.getCustomer().getAdminEmailList(),
                    "✅ Staff Onboarding Complete: " + user.getName(),
                    adminMsg
                );
            }
        });
    }

    @Override
    @Transactional
    public void resendLink(String email) {
        User user = userRepository.findByEmailAndIsActiveTrue(email).orElse(null);
        if (user == null) {
            log.info("Resend link requested for non-existent or inactive email: {}", email);
            return;
        }

        if (!"PENDING".equals(user.getPasswordStatus())) {
            // Already set, just silently succeed to prevent enum
            return;
        }

        // Invalidate active tokens
        List<UserToken> activeTokens = userTokenRepository.findByUserIdAndIsInvalidatedFalseAndUsedAtNull(user.getId());
        for (UserToken t : activeTokens) {
            t.setIsInvalidated(true);
        }
        userTokenRepository.saveAll(activeTokens);

        // Gen new token
        String tokenStr = SecureTokenGenerator.generateToken();
        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setToken(tokenStr);
        userToken.setCreatedAt(LocalDateTime.now());
        userToken.setExpiresAt(LocalDateTime.now().plusHours(appProperties.getToken().getExpiryHours()));
        userTokenRepository.save(userToken);

        // Email
        String adminUrl = appProperties.getCustomer().getAdminUrl();
        String magicLink = adminUrl + "/set-password?token=" + tokenStr;

        emailTemplateRepository.findBySlugAndIsActiveTrueAndDeletedAtNull("password-setup").ifPresent(template -> {
            String msg = template.getMessage();
            if (msg != null) {
                msg = msg.replace("{{userName}}", user.getName())
                         .replace("{{userEmail}}", user.getEmail())
                         .replace("{{magicLink}}", magicLink)
                         .replace("{{expiryHours}}", String.valueOf(appProperties.getToken().getExpiryHours()))
                         .replace("{{year}}", String.valueOf(LocalDateTime.now().getYear()));
                emailHelper.sendEmail(user.getEmail(), "Set up your CineQ Admin password", msg);
            }
        });
    }

private LinkedHashMap<String, String> resolveActiveRoleNamesById(List<UserHasRole> userRoles) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        if (userRoles == null || userRoles.isEmpty()) {
            return ordered;
        }

        List<ObjectId> roleObjectIds = new ArrayList<>();
        for (UserHasRole ur : userRoles) {
            String roleId = ur.getRoleId();
            if (roleId == null || roleId.isBlank()) {
                continue;
            }
            try {
                roleObjectIds.add(new ObjectId(roleId));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed ObjectId references in user_has_roles
            }
        }

        if (roleObjectIds.isEmpty()) {
            return ordered;
        }

        Query roleQuery = Query.query(
                Criteria.where("_id").in(roleObjectIds)
                        .and("isActive").is(true)
                        .and("deletedAt").is(null)
        );

        List<Map> activeRoles = mongoTemplate.find(roleQuery, Map.class, "roles");
        Map<String, String> activeRoleNameById = new LinkedHashMap<>();
        for (Map roleDoc : activeRoles) {
            if (roleDoc.get("_id") != null) {
                String roleId = roleDoc.get("_id").toString();
                String roleName = roleDoc.get("name") != null ? roleDoc.get("name").toString() : "USER";
                activeRoleNameById.put(roleId, roleName);
            }
        }

        for (UserHasRole ur : userRoles) {
            String roleId = ur.getRoleId();
            if (activeRoleNameById.containsKey(roleId)) {
                ordered.put(roleId, activeRoleNameById.get(roleId));
            }
        }
        return ordered;
    }

    /**
     * Maps a User entity to UserDTO with role information.
     */
    private UserDTO mapUserToDTO(User user, List<String> roleIds, List<String> roleNames) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPasswordStatus(user.getPasswordStatus());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRoleIds(roleIds != null ? roleIds : List.of());
        dto.setRoleNames(roleNames != null ? roleNames : List.of("USER"));
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
            User user = userRepository.findByEmailAndIsActiveTrue(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            log.debug("getProfile: Found user - id={}, name={}", user.getId(), user.getName());

            List<UserHasRole> activeUserRoles = userHasRoleRepository.findByUserIdActive(user.getId());
            LinkedHashMap<String, String> activeRoleNameById = resolveActiveRoleNamesById(activeUserRoles);
            List<String> roleNames = new ArrayList<>(activeRoleNameById.values());
            List<ProfileResponse.RoleInfo> roleInfos = buildActiveRoleInfos(activeRoleNameById);
            String prominentRoleName = applicationProperties.getSecurity().getProminentRole();

            if (roleNames.contains(prominentRoleName)) {
                log.info("User has prominent role '{}'. Granting access to all enabled modules.", prominentRoleName);
                List<Integer> allPermissionIds = Arrays.stream(FormAction.values())
                        .map(FormAction::getCode)
                        .collect(Collectors.toList());

                List<ProfileResponse.ModuleInfo> allModules = getAllEnabledModules(allPermissionIds);
                return buildProfileResponse(user, roleInfos, allModules);
            }

            List<ProfileResponse.ModuleInfo> modules = new ArrayList<>();

            Query roleHasModulesQuery = Query.query(
                    new Criteria().andOperator(
                            new Criteria().orOperator(
                                    Criteria.where("roleId").in(activeRoleNameById.keySet()),
                                    Criteria.where("role_id").in(activeRoleNameById.keySet())
                            ),
                            new Criteria().orOperator(
                                    Criteria.where("deletedAt").is(null),
                                    Criteria.where("deleted_at").is(null)
                            )
                    )
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> roleHasModules = (List<Map<String, Object>>) (List<?>)
                mongoTemplate.find(roleHasModulesQuery, Map.class, "role_has_modules");

            for (Map<String, Object> rhm : roleHasModules) {
                String moduleId = rhm.get("moduleId") != null
                        ? rhm.get("moduleId").toString()
                        : rhm.get("module_id") != null
                        ? rhm.get("module_id").toString()
                        : null;

                if (moduleId == null) continue;

                Query moduleQuery = Query.query(
                        Criteria.where("_id").is(new org.bson.types.ObjectId(moduleId))
                                .and("deletedAt").is(null)
                );

                @SuppressWarnings("unchecked")
                Map<String, Object> moduleDoc = mongoTemplate.findOne(moduleQuery, Map.class, "modules");

                if (moduleDoc != null) {
                    @SuppressWarnings("unchecked")
                    List<Object> permissionIds = rhm.get("permissionIds") instanceof List
                            ? (List<Object>) rhm.get("permissionIds")
                            : rhm.get("permission_ids") instanceof List
                            ? (List<Object>) rhm.get("permission_ids")
                            : List.of();

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
                            .parentId(
                                    moduleDoc.get("parent_id") != null
                                            ? moduleDoc.get("parent_id").toString()
                                            : moduleDoc.get("parentId") != null
                                            ? moduleDoc.get("parentId").toString()
                                            : null
                            )
                            .build();

                    if (!modules.stream().anyMatch(m -> m.getId().equals(moduleInfo.getId()))) {
                        modules.add(moduleInfo);
                    }
                }
            }

            return buildProfileResponse(user, roleInfos, modules);

        } catch (ResourceNotFoundException e) {
            log.error("getProfile: User not found - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getProfile ERROR: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to fetch profile");
        }
    }

    private List<ProfileResponse.RoleInfo> buildActiveRoleInfos(Map<String, String> activeRoleNameById) {
        if (activeRoleNameById == null || activeRoleNameById.isEmpty()) {
            return List.of();
        }

        return activeRoleNameById.entrySet().stream()
                .map(entry -> ProfileResponse.RoleInfo.builder()
                        .id(entry.getKey())
                        .name(entry.getValue())
                        .isActive(true)
                        .build())
                .toList();
    }

    private List<ProfileResponse.ModuleInfo> getAllEnabledModules(List<Integer> permissionIds) {
        Query allModulesQuery = Query.query(Criteria.where("is_enabled").is(true).and("deletedAt").is(null));
        List<Map> allModuleDocs = mongoTemplate.find(allModulesQuery, Map.class, "modules");

        List<Object> objectPermissionIds = new ArrayList<>(permissionIds);

        return allModuleDocs.stream().map(moduleDoc ->
                ProfileResponse.ModuleInfo.builder()
                        .id(moduleDoc.get("_id").toString())
                        .code(moduleDoc.get("code") instanceof Integer
                                ? (Integer) moduleDoc.get("code")
                                : Integer.parseInt(moduleDoc.get("code").toString()))
                        .name((String) moduleDoc.get("name"))
                        .displayName((String) moduleDoc.get("display_name"))
                        .api((String) moduleDoc.get("api"))
                        .description((String) moduleDoc.get("description"))
                        .isEnabled(true)
                        .parentId(
                                moduleDoc.get("parent_id") != null
                                        ? moduleDoc.get("parent_id").toString()
                                        : moduleDoc.get("parentId") != null
                                        ? moduleDoc.get("parentId").toString()
                                        : null
                        )
                        .permissionIds(objectPermissionIds)
                        .build()
        ).collect(Collectors.toList());
    }

    private ProfileResponse buildProfileResponse(User user, List<ProfileResponse.RoleInfo> roleInfos, List<ProfileResponse.ModuleInfo> modules) {
        ProfileResponse response = ProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .role(roleInfos)
                .modules(modules)
                .build();
        log.info("getProfile END: email={}, activeRoleCount={}, moduleCount={}", user.getEmail(), roleInfos.size(), modules.size());
        return response;
    }


}
