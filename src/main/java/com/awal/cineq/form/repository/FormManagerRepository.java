package com.awal.cineq.form.repository;

import com.awal.cineq.form.model.FormManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB Repository for FormManager
 */
@Repository
public interface FormManagerRepository extends MongoRepository<FormManager, String> {

    // Find by slug (exclude soft-deleted)
    @Query("{ 'slug': ?0, 'deletedAt': null }")
    Optional<FormManager> findBySlug(String slug);

    // Count by slug (exclude soft-deleted) - safer than existsBySlug for MongoDB
    @Query(value = "{ 'slug': ?0, 'deletedAt': null }", count = true)
    long countBySlug(String slug);

    // Find by title containing (exclude soft-deleted)
    @Query("{ 'title': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    Page<FormManager> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Find all active forms (not soft-deleted), sorted by creation date
    @Query("{ 'deletedAt': null }")
    Page<FormManager> findAllActive(Pageable pageable);

    /**
     * Find all active form managers with projection (listing optimization)
     * Excludes heavy fields: formSteps
     * Only fetches: id, slug, title, description, modelName, isActive, moduleCode, createdAt, updatedAt
     */
    @Query(value = "{ 'deletedAt': null }", fields = "{ 'formSteps': 0 }")
    Page<FormManager> findAllActiveForListing(Pageable pageable);

    /**
     * Search by title with projection (listing optimization)
     * Excludes heavy fields: formSteps
     */
    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' }, 'deletedAt': null }", fields = "{ 'formSteps': 0 }")
    Page<FormManager> findByTitleContainingIgnoreCaseForListing(String title, Pageable pageable);

    // Find by module code (exclude soft-deleted)
    @Query("{ 'moduleCode': ?0, 'deletedAt': null }")
    Optional<FormManager> findByModuleCodeAndDeletedAtIsNull(Integer moduleCode);

    // Count forms with specific module code (exclude soft-deleted)
    @Query(value = "{ 'moduleCode': ?0, 'deletedAt': null }", count = true)
    long countByModuleCodeAndDeletedAtIsNull(Integer moduleCode);
}

