package com.awal.cineq.user.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.user.dto.UserDTO;
import com.awal.cineq.user.dto.request.UserPageRequest;
import com.awal.cineq.user.dto.request.UserUpdateRequest;

import java.util.List;

/**
 * Service interface for User management operations.
 * Note: User creation is handled by AuthService.register()
 */
public interface UserService {

    /**
     * Get paginated list of users with optional search and filters.
     *
     * @param userPageRequest Pagination and filter parameters
     * @return Paginated response of users
     */
    PaginationResponse<UserDTO> getUsers(UserPageRequest userPageRequest);

    /**
     * Get a single user by ID.
     *
     * @param id User's MongoDB ObjectId
     * @return User details
     */
    UserDTO getUserById(String id);

    /**
     * Update user details.
     *
     * @param id User's MongoDB ObjectId
     * @param updateRequest Updated user data
     * @return Updated user details
     */
    UserDTO updateUser(String id, UserUpdateRequest updateRequest);

    /**
     * Soft delete a user.
     *
     * @param id User's MongoDB ObjectId
     */
    void deleteUser(String id);

    /**
     * Bulk enable or disable users.
     *
     * @param ids List of user IDs
     * @param enabled true to enable, false to disable
     */
    void bulkUpdateUserStatus(List<String> ids, boolean enabled);
}
