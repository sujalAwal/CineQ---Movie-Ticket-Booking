package com.awal.cineq.user.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.DuplicateResourceException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.user.dto.UserDTO;
import com.awal.cineq.user.dto.request.UserPageRequest;
import com.awal.cineq.user.dto.request.UserUpdateRequest;
import com.awal.cineq.user.model.User;
import com.awal.cineq.user.repository.UserRepository;
import com.awal.cineq.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * User Service Implementation using MongoDB.
 * Handles all business logic for user management.
 * Note: User creation is handled by AuthService.register()
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<UserDTO> getUsers(UserPageRequest request) {
        log.info("getUsers STARTED: request={}", request);
        try {
            PageRequest pageRequest = request.toPageRequest();

            Page<User> users = findUsers(request, pageRequest);
            Long total = users.getTotalElements();
            log.debug("Total users found: {}", total);

            List<UserDTO> result = users.stream()
                    .map(this::mapUserToDTO)
                    .toList();

            log.debug("getUsers result count: {}", result.size());
            log.info("getUsers END");

            return PaginationResponse.success(
                    "Users fetched successfully",
                    result,
                    users.getNumber() + 1, // converting to 1-based page index
                    users.getSize(),
                    users.getTotalPages(),
                    users.getTotalElements(),
                    users.hasNext(),
                    users.hasPrevious()
            );

        } catch (Exception e) {
            log.error("getUsers ERROR", e);
            throw new BusinessException("Failed to fetch users", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(String id) {
        log.info("getUserById STARTED: id={}", id);
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

            // Ensure user is not soft-deleted
            if (user.getDeletedAt() != null) {
                throw new ResourceNotFoundException("User not found with id: " + id);
            }

            UserDTO result = mapUserToDTO(user);
            log.debug("getUserById result: {}", result);
            log.info("getUserById END");

            return result;
        } catch (ResourceNotFoundException e) {
            log.error("getUserById NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getUserById ERROR", e);
            throw new BusinessException("Failed to fetch user by id", e);
        }
    }

    @Override
    public UserDTO updateUser(String id, UserUpdateRequest updateRequest) {
        log.info("updateUser STARTED: id={}", id);
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

            // Ensure user is not soft-deleted
            if (user.getDeletedAt() != null) {
                throw new ResourceNotFoundException("User not found with id: " + id);
            }

            // Update fields if provided
            if (updateRequest.getName() != null && !updateRequest.getName().isBlank()) {
                user.setName(updateRequest.getName());
            }

            if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank()) {
                // Check if email is already taken by another user
                if (!user.getEmail().equals(updateRequest.getEmail())) {
                    Optional<User> existingUser = userRepository.findByEmail(updateRequest.getEmail());
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                        throw new DuplicateResourceException("Email already exists: " + updateRequest.getEmail());
                    }
                    user.setEmail(updateRequest.getEmail());
                }
            }

            if (updateRequest.getPhoneNumber() != null) {
                user.setPhoneNumber(updateRequest.getPhoneNumber());
            }

            if (updateRequest.getRoleId() != null && !updateRequest.getRoleId().isBlank()) {
                // Validate that the role exists
                validateRoleExists(updateRequest.getRoleId());
                user.setRoleId(updateRequest.getRoleId());
            }

            if (updateRequest.getIsActive() != null) {
                user.setIsActive(updateRequest.getIsActive());
            }

            User updated = userRepository.save(user);
            UserDTO result = mapUserToDTO(updated);

            log.debug("updateUser result: {}", result);
            log.info("updateUser END");

            return result;
        } catch (ResourceNotFoundException | DuplicateResourceException | BadRequestException e) {
            log.error("updateUser VALIDATION ERROR: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("updateUser ERROR", e);
            throw new BusinessException("Failed to update user", e);
        }
    }

    @Override
    public void deleteUser(String id) {
        log.info("deleteUser STARTED: id={}", id);
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

            // Ensure user is not already soft-deleted
            if (user.getDeletedAt() != null) {
                throw new ResourceNotFoundException("User not found with id: " + id);
            }

            user.softDelete(); // Sets deletedAt and isActive = false
            userRepository.save(user);

            log.info("deleteUser END: id={}", id);
        } catch (ResourceNotFoundException e) {
            log.error("deleteUser NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("deleteUser ERROR", e);
            throw new BusinessException("Failed to delete user", e);
        }
    }

    @Override
    @Transactional
    public void bulkUpdateUserStatus(List<String> ids, boolean enabled) {
        log.info("bulkUpdateUserStatus STARTED: count={}, enabled={}", ids.size(), enabled);
        try {
            List<User> users = userRepository.findAllById(ids);

            if (users.size() != ids.size()) {
                throw new ResourceNotFoundException("Some users not found for the provided IDs");
            }

            // Filter out soft-deleted users
            List<User> activeUsers = users.stream()
                    .filter(u -> u.getDeletedAt() == null)
                    .toList();

            if (activeUsers.isEmpty()) {
                throw new ResourceNotFoundException("No active users found for the provided IDs");
            }

            for (User user : activeUsers) {
                user.setIsActive(enabled);
            }

            userRepository.saveAll(activeUsers);
            log.info("bulkUpdateUserStatus END: updated={}", activeUsers.size());
        } catch (ResourceNotFoundException e) {
            log.error("bulkUpdateUserStatus NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("bulkUpdateUserStatus ERROR", e);
            throw new BusinessException("Failed to bulk update users", e);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Find users based on request filters using MongoTemplate for complex queries.
     */
    private Page<User> findUsers(UserPageRequest request, PageRequest pageRequest) {
        log.debug("findUsers STARTED: hasSearch={}, hasRoleId={}", request.hasSearch(), request.hasRoleId());

        Query query = new Query();

        // Always exclude soft-deleted users
        query.addCriteria(Criteria.where("deletedAt").is(null));

        // Filter by active status if provided
        if (request.getActive() != null) {
            query.addCriteria(Criteria.where("isActive").is(request.getActive()));
        }

        // Filter by roleId if provided
        if (request.hasRoleId()) {
            query.addCriteria(Criteria.where("roleId").is(request.getRoleId()));
        }

        // Search by name or email if provided
        if (request.hasSearch()) {
            String searchPattern = request.getSearch();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(searchPattern, "i"),
                    Criteria.where("email").regex(searchPattern, "i")
            ));
        }

        // Get total count
        long total = mongoTemplate.count(query, User.class);

        // Apply pagination
        query.with(pageRequest);

        List<User> users = mongoTemplate.find(query, User.class);

        return PageableExecutionUtils.getPage(users, pageRequest, () -> total);
    }

    /**
     * Maps a User entity to UserDTO, including role name from roles collection.
     */
    private UserDTO mapUserToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRoleId(user.getRoleId());
        dto.setRoleName(fetchRoleNameById(user.getRoleId()));
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    /**
     * Fetches the role name from the roles collection by roleId.
     */
    private String fetchRoleNameById(String roleId) {
        if (roleId == null || roleId.isBlank()) {
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
                return roleDoc.get("name").toString();
            }
            return "USER";
        } catch (IllegalArgumentException e) {
            log.warn("fetchRoleNameById: Invalid ObjectId format for roleId={}", roleId);
            return "USER";
        }
    }

    /**
     * Validates that a role with the given ID exists in the roles collection.
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
                throw new ResourceNotFoundException("Role not found with ID: " + roleId);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role ID format: " + roleId);
        }
    }
}
