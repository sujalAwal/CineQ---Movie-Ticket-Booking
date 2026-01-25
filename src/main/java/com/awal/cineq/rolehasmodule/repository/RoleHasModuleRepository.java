package com.awal.cineq.rolehasmodule.repository;

import com.awal.cineq.rolehasmodule.model.RoleHasModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB Repository for RoleHasModule
 * Extends MongoRepository for MongoDB operations
 */
@Repository
public interface RoleHasModuleRepository extends MongoRepository<RoleHasModule, String> {

    // Find by role ID and module ID (excluding soft-deleted)
    @Query("{ 'roleId': ?0, 'moduleId': ?1, 'deletedAt': null }")
    Optional<RoleHasModule> findByRoleIdAndModuleId(String roleId, String moduleId);

    // Find all by role ID (excluding soft-deleted)
    @Query("{ 'roleId': ?0, 'deletedAt': null }")
    Page<RoleHasModule> findByRoleId(String roleId, Pageable pageable);

    // Find all by module ID (excluding soft-deleted)
    @Query("{ 'moduleId': ?0, 'deletedAt': null }")
    Page<RoleHasModule> findByModuleId(String moduleId, Pageable pageable);

    // Search by role name (case-insensitive, excluding soft-deleted)
    @Query("{ 'role': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    Page<RoleHasModule> findByRoleContainingIgnoreCase(String role, Pageable pageable);

    // Search by module name (case-insensitive, excluding soft-deleted)
    @Query("{ 'module': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    Page<RoleHasModule> findByModuleContainingIgnoreCase(String module, Pageable pageable);

    // Search by role and module (excluding soft-deleted)
    @Query("{ 'role': { $regex: ?0, $options: 'i' }, 'module': { $regex: ?1, $options: 'i' }, 'deletedAt': null }")
    Page<RoleHasModule> findByRoleAndModuleContainingIgnoreCase(String role, String module, Pageable pageable);

    // Check if role-module permission exists (excluding soft-deleted)
    @Query("{ 'roleId': ?0, 'moduleId': ?1, 'deletedAt': null }")
    boolean existsByRoleIdAndModuleId(String roleId, String moduleId);

    // Find all active role-module permissions (not soft-deleted)
    @Query("{ 'deletedAt': null }")
    Page<RoleHasModule> findAll(Pageable pageable);
}
