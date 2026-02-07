package com.awal.cineq.module.repository;

import com.awal.cineq.module.model.Module;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Module Repository - MongoDB queries for Module collection
 * All queries exclude soft-deleted records (deletedAt != null)
 */
@Repository
public interface ModuleRepository extends MongoRepository<Module, String> {

    /**
     * Find by code (exclude soft-deleted)
     * Used for code-based lookups
     */
    @Query("{ 'code': ?0, 'deletedAt': null }")
    Optional<Module> findByCodeAndDeletedAtIsNull(Integer code);

    /**
     * Check if code exists (exclude soft-deleted)
     * Used for duplicate validation during creation
     */
    @Query("{ 'code': ?0, 'deletedAt': null }")
    Boolean existsByCodeAndDeletedAtIsNull(Integer code);

    /**
     * Find by ID (exclude soft-deleted)
     */
    @Query("{ '_id': ?0, 'deletedAt': null }")
    Optional<Module> findByIdAndDeletedAtIsNull(String id);

    /**
     * Paginated list (exclude soft-deleted)
     * Default sort by createdAt DESC applied in service layer
     */
    @Query("{ 'deletedAt': null }")
    Page<Module> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * Search by name pattern (case-insensitive, exclude soft-deleted)
     * Used for filtering modules by name
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    Page<Module> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    /**
     * Find by IDs (for bulk operations)
     * Exclude soft-deleted records
     */
    @Query("{ '_id': { '$in': ?0 }, 'deletedAt': null }")
    List<Module> findAllByIdInAndDeletedAtIsNull(List<String> ids);

    @Aggregation(pipeline = {
            "{ '$match': { 'deletedAt': null } }",
            "{ '$sort': { 'code': -1 } }",
            "{ '$limit': 1 }"
    })
    Optional<Module> findMaxCode();

    /**
     * Find all active parent modules (top-level modules)
     * Conditions: isEnabled=true, parentId=null, api=null, deletedAt=null
     * Used for navigation menus and parent module selection
     */
    @Query("{ 'is_enabled': true, 'parent_id': { $in: [null, ''] }, 'api': { $in: [null, ''] }, 'deletedAt': null }")
    List<Module> findAllActiveParentModules();

    /**
     * Find all enabled modules (excluding soft-deleted)
     * Used when user has prominent role (SUPERADMIN, ADMIN, etc.)
     */
    @Query("{ 'is_enabled': true, 'deletedAt': null }")
    List<Module> findAllByIsEnabledTrueAndDeletedAtIsNull();

    /**
     * Find modules by IDs, only enabled ones (excluding soft-deleted)
     * Used for role-based module access with enabled check
     */
    @Query("{ '_id': { '$in': ?0 }, 'is_enabled': true, 'deletedAt': null }")
    List<Module> findAllByIdInAndIsEnabledTrueAndDeletedAtIsNull(List<String> ids);
}