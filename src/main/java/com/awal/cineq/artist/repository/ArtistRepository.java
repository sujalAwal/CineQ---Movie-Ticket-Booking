package com.awal.cineq.artist.repository;

import com.awal.cineq.artist.model.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Artist
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface ArtistRepository extends MongoRepository<Artist, String> {

    // Find active artists only (soft-delete filter)
    @Query("{ 'deletedAt': null, 'isActive': ?0 }")
    List<Artist> findByIsActive(Boolean isActive);

    // Find all active artists (soft-delete filter)
    @Query("{ 'deletedAt': null }")
    List<Artist> findAllActive();

    // Find by ID with soft-delete filter
    @Query("{ '_id': ?0, 'deletedAt': null }")
    Optional<Artist> findByIdActive(String id);

    // Find by name with soft-delete filter
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    Page<Artist> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
