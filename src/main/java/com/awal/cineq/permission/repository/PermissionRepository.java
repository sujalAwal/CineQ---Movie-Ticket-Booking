package com.awal.cineq.permission.repository;

import com.awal.cineq.permission.model.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Permission Repository - MongoDB queries for Permission collection
 * All queries exclude soft-deleted records (deletedAt != null)
 */
@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {

    /**
     * Find by module ID (exclude soft-deleted)
     * Used to get all permissions for a specific module
     */
    @Query("{ 'moduleId': ?0, 'deletedAt': null }")
    List<Permission> findByModuleIdAndDeletedAtIsNull(String moduleId);

    /**
     * Find by code (exclude soft-deleted)
     * Used for code-based lookups
     */
    @Query("{ 'code': ?0, 'deletedAt': null }")
    Optional<Permission> findByCodeAndDeletedAtIsNull(String code);

    /**
     * Check if code exists (exclude soft-deleted)
     * Used for duplicate validation
     */
    @Query("{ 'code': ?0, 'deletedAt': null }")
    boolean existsByCodeAndDeletedAtIsNull(String code);

    /**
     * Find by ID (exclude soft-deleted)
     */
    @Query("{ '_id': ?0, 'deletedAt': null }")
    Optional<Permission> findByIdAndDeletedAtIsNull(String id);

    /**
     * Paginated list (exclude soft-deleted)
     */
    @Query("{ 'deletedAt': null }")
    Page<Permission> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * Find by IDs (for bulk operations)
     * Exclude soft-deleted records
     */
    @Query("{ '_id': { '$in': ?0 }, 'deletedAt': null }")
    List<Permission> findAllByIdInAndDeletedAtIsNull(List<String> ids);

    /**
     * Count permissions for a module (exclude soft-deleted)
     * Used to display permission count in module responses
     */
    @Query(value = "{ 'moduleId': ?0, 'deletedAt': null }", count = true)
    long countByModuleIdAndDeletedAtIsNull(String moduleId);

    /**
     * Find by module code (exclude soft-deleted)
     * Used for filtering permissions by module numeric code
     */
    @Query("{ 'moduleCode': ?0, 'deletedAt': null }")
    List<Permission> findByModuleCodeAndDeletedAtIsNull(Integer moduleCode);

    /**
     * Find by action code (exclude soft-deleted)
     * Used for filtering permissions by action
     */
    @Query("{ 'actionCode': ?0, 'deletedAt': null }")
    List<Permission> findByActionCodeAndDeletedAtIsNull(String actionCode);
}
