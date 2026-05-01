package com.awal.cineq.genre.repository;

import com.awal.cineq.genre.model.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB Repository for Genre
 * Extends MongoRepository instead of JpaRepository
 */
@Repository
public interface GenreRepository extends MongoRepository<Genre, String> {

    // Find by name (case-insensitive) excluding soft-deleted
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    Page<Genre> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Find by name exact match
    @Query("{ 'name': ?0, 'deletedAt': null }")
    Optional<Genre> findByName(String name);

    // Check if genre exists (exclude soft-deleted)
    @ExistsQuery("{ 'name': ?0, 'deletedAt': null }")
    Boolean existsByName(String name);

    // Find all active genres (not soft-deleted)
    @Query("{ 'deletedAt': null }")
    Page<Genre> findAll(Pageable pageable);
}