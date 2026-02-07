/**
 * REST Controller for User management endpoints.
 * Provides CRUD operations for users (except create, which is handled by AuthController).
 *
 * Endpoints:
 * - GET    /user          - List users with pagination, search, and filters
 * - GET    /user/{id}     - Get user by ID
 * - PUT    /user/{id}     - Update user
 * - DELETE /user/{id}     - Soft delete user
 * - POST   /user/bulk-enable  - Bulk enable users
 * - POST   /user/bulk-disable - Bulk disable users
 */
package com.awal.cineq.user.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.user.dto.UserDTO;
import com.awal.cineq.user.dto.request.BulkUserStatusUpdateRequest;
import com.awal.cineq.user.dto.request.UserPageRequest;
import com.awal.cineq.user.dto.request.UserUpdateRequest;
import com.awal.cineq.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User management endpoints.
 * Provides CRUD operations for users (except create, which is handled by AuthController).
 *
 * Endpoints:
 * - GET    /user          - List users with pagination, search, and filters
 * - GET    /user/{id}     - Get user by ID
 * - PUT    /user/{id}     - Update user
 * - DELETE /user/{id}     - Soft delete user
 * - POST   /user/bulk-enable  - Bulk enable users
 * - POST   /user/bulk-disable - Bulk disable users
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("user")
@Slf4j
@Tag(name = "User Management", description = "User CRUD operations (except create)")
public class UserController {

    private final UserService userService;

    /**
     * Get paginated list of users with optional search and filters.
     *
     * @param userRequest Pagination and filter parameters
     * @return Paginated response of users
     */
    @GetMapping(path = {"", "/"})
    @Operation(summary = "List users", description = "Get paginated list of users with optional search and filters")
    public PaginationResponse<UserDTO> getAllUsers(@Valid UserPageRequest userRequest) {
        log.info("getAllUsers STARTED");
        try {
            PaginationResponse<UserDTO> response = userService.getUsers(userRequest);
            log.info("getAllUsers END");
            return response;
        } catch (Exception e) {
            log.error("getAllUsers ERROR", e);
            throw e;
        }
    }

    /**
     * Get a single user by ID.
     *
     * @param id User's MongoDB ObjectId
     * @return User details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Fetch a single user by their ID")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("getUserById STARTED: id={}", id);
        try {
            UserDTO user = userService.getUserById(id);
            ApiResponse<UserDTO> response = ApiResponse.success("User fetched successfully", user);
            response.setPath(request.getRequestURI());
            log.info("getUserById END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getUserById ERROR", e);
            throw e;
        }
    }

    /**
     * Update user details.
     *
     * @param id User's MongoDB ObjectId
     * @param updateRequest Updated user data
     * @return Updated user details
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update user details (name, email, phone, role, status)")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest updateRequest,
            HttpServletRequest request) {
        log.info("updateUser STARTED: id={}", id);
        try {
            UserDTO updated = userService.updateUser(id, updateRequest);
            ApiResponse<UserDTO> response = ApiResponse.success("User updated successfully", updated);
            response.setPath(request.getRequestURI());
            log.info("updateUser END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("updateUser ERROR", e);
            throw e;
        }
    }

    /**
     * Soft delete a user.
     *
     * @param id User's MongoDB ObjectId
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft delete a user (sets deletedAt timestamp)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("deleteUser STARTED: id={}", id);
        try {
            userService.deleteUser(id);
            ApiResponse<Void> response = ApiResponse.success("User deleted successfully", null);
            response.setPath(request.getRequestURI());
            log.info("deleteUser END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("deleteUser ERROR", e);
            throw e;
        }
    }

    /**
     * Bulk enable users.
     *
     * @param bulkRequest Request containing list of user IDs
     */
    @PostMapping("/bulk-enable")
    @Operation(summary = "Bulk enable users", description = "Enable multiple users at once")
    public ResponseEntity<ApiResponse<Void>> bulkEnableUsers(
            @RequestBody BulkUserStatusUpdateRequest bulkRequest,
            HttpServletRequest request) {
        log.info("bulkEnableUsers STARTED");
        try {
            userService.bulkUpdateUserStatus(bulkRequest.getIds(), true);
            ApiResponse<Void> response = ApiResponse.success("Users enabled successfully", null);
            response.setPath(request.getRequestURI());
            log.info("bulkEnableUsers END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkEnableUsers ERROR", e);
            throw e;
        }
    }

    /**
     * Bulk disable users.
     *
     * @param bulkRequest Request containing list of user IDs
     */
    @PostMapping("/bulk-disable")
    @Operation(summary = "Bulk disable users", description = "Disable multiple users at once")
    public ResponseEntity<ApiResponse<Void>> bulkDisableUsers(
            @RequestBody BulkUserStatusUpdateRequest bulkRequest,
            HttpServletRequest request) {
        log.info("bulkDisableUsers STARTED");
        try {
            userService.bulkUpdateUserStatus(bulkRequest.getIds(), false);
            ApiResponse<Void> response = ApiResponse.success("Users disabled successfully", null);
            response.setPath(request.getRequestURI());
            log.info("bulkDisableUsers END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkDisableUsers ERROR", e);
            throw e;
        }
    }
}
