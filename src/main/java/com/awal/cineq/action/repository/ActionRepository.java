package com.awal.cineq.action.repository;

import com.awal.cineq.action.model.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Action Repository - MongoDB queries for Action collection
 * All queries exclude soft-deleted records (deletedAt != null)
 */
@Repository
public interface ActionRepository extends MongoRepository<Action, String> {

    /**
     * Find all active actions (enabled and not soft-deleted)
     * Used for permission generation when creating modules
     */
    @Query("{ 'isEnabled': true, 'deletedAt': null }")
    List<Action> findAllActive();

    /**
     * Find by code (exclude soft-deleted)
     * Used for code-based lookups
     */
//    @Query("{ 'code': ?0, 'deletedAt': null }")
//    Optional<Action> findByCodeAndDeletedAtIsNull(String code);

    /**
     * Find by code (exclude soft-deleted)
     */
    Optional<Action> findByCodeAndDeletedAtIsNull(String code);

    /**
     * Check if code exists (exclude soft-deleted)
     * Used for duplicate validation during creation
     */
    @Query("{ 'code': ?0, 'deletedAt': null }")
    boolean existsByCodeAndDeletedAtIsNull(String code);

    /**
     * Find by ID (exclude soft-deleted)
     */
    @Query("{ '_id': ?0, 'deletedAt': null }")
    Optional<Action> findByIdAndDeletedAtIsNull(String id);

    /**
     * Paginated list (exclude soft-deleted)
     * Used for listing all actions with pagination
     */
    @Query("{ 'deletedAt': null }")
    Page<Action> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * Find by IDs (for bulk operations)
     * Exclude soft-deleted records
     */
    @Query("{ '_id': { '$in': ?0 }, 'deletedAt': null }")
    List<Action> findAllByIdInAndDeletedAtIsNull(List<String> ids);

    /**
     * Search by name pattern (case-insensitive, exclude soft-deleted)
     */
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    Page<Action> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);
}
