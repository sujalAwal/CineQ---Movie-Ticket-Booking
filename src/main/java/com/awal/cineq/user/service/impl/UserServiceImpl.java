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
import com.awal.cineq.user.model.UserHasRole;
import com.awal.cineq.user.repository.UserRepository;
import com.awal.cineq.user.repository.UserHasRoleRepository;
import com.awal.cineq.user.service.UserRoleAssigner;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserHasRoleRepository userHasRoleRepository;
    private final MongoTemplate mongoTemplate;
    private final UserRoleAssigner userRoleAssigner;

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

            if (user.getDeletedAt() != null) {
                throw new ResourceNotFoundException("User not found with id: " + id);
            }

            if (updateRequest.getName() != null && !updateRequest.getName().isBlank()) {
                user.setName(updateRequest.getName());
            }

            if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank()) {
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

            if (updateRequest.getIsActive() != null) {
                user.setIsActive(updateRequest.getIsActive());
            }

            User updated = userRepository.save(user);

            if (updateRequest.getRoleIds() != null && !updateRequest.getRoleIds().isEmpty()) {
                userRoleAssigner.replaceUserRoles(updated.getId(), updateRequest.getRoleIds());
            }

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
        log.debug("findUsers STARTED: hasSearch={}", request.hasSearch());

        Query query = new Query();

        query.addCriteria(Criteria.where("deletedAt").is(null));

        if (request.getActive() != null) {
            query.addCriteria(Criteria.where("isActive").is(request.getActive()));
        }

        if (request.hasSearch()) {
            String searchPattern = request.getSearch();
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(searchPattern, "i"),
                    Criteria.where("email").regex(searchPattern, "i")
            ));
        }

        if (request.hasRoleId()) {
            List<String> userIdsWithRole = userHasRoleRepository.findByRoleIdActive(request.getRoleId()).stream()
                    .map(UserHasRole::getUserId)
                    .distinct()
                    .toList();
            if (userIdsWithRole.isEmpty()) {
                return Page.empty(pageRequest);
            }
            query.addCriteria(Criteria.where("_id").in(userIdsWithRole));
        }

        long total = mongoTemplate.count(query, User.class);
        query.with(pageRequest);

        List<User> users = mongoTemplate.find(query, User.class);

        return PageableExecutionUtils.getPage(users, pageRequest, () -> total);
    }

    /**
     * Maps a User entity to UserDTO with all assigned roles.
     */
    private UserDTO mapUserToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());

        List<UserHasRole> userRoles = userHasRoleRepository.findByUserIdActive(user.getId());

        if (userRoles.isEmpty()) {
            dto.setRoleIds(List.of());
            dto.setRoleNames(List.of("USER"));
        } else {
            dto.setRoleIds(userRoles.stream().map(UserHasRole::getRoleId).collect(Collectors.toList()));
            dto.setRoleNames(userRoles.stream()
                    .map(ur -> ur.getRoleName() != null ? ur.getRoleName() : "USER")
                    .collect(Collectors.toList()));
        }

        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setPasswordStatus(user.getPasswordStatus());
        return dto;
    }
}
