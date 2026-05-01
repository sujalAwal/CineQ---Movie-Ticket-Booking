package com.awal.cineq.screen.repository;

import com.awal.cineq.screen.model.Screen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Screen entity
 * Provides database operations for screens collection
 */
@Repository
public interface ScreenRepository extends MongoRepository<Screen, String> {

    /**
     * Find all active screens by theatre ID
     * @param theatreId Theatre identifier
     * @return List of screens for the theatre
     */
    @Query("{ 'theatreId': ?0, 'isActive': true, 'deletedAt': null }")
    List<Screen> findByTheatreIdActive(String theatreId);

    /**
     * Find all screens by theatre ID (paginated)
     * @param theatreId Theatre identifier
     * @param pageable Pagination parameters
     * @return Page of screens
     */
    @Query("{ 'theatreId': ?0, 'isActive': true, 'deletedAt': null }")
    Page<Screen> findByTheatreIdActivePaginated(String theatreId, Pageable pageable);

    /**
     * Find screen by ID with soft delete filter
     * @param id Screen identifier
     * @return Optional containing screen if found
     */
    @Query("{ '_id': ?0, 'isActive': true, 'deletedAt': null }")
    Optional<Screen> findByIdActive(String id);

    /**
     * Find all active screens
     * @return List of all active screens
     */
    @Query("{ 'isActive': true, 'deletedAt': null }")
    List<Screen> findAllActive();

    /**
     * Find all active screens (paginated)
     * @param pageable Pagination parameters
     * @return Page of screens
     */
    @Query("{ 'isActive': true, 'deletedAt': null }")
    Page<Screen> findAllActivePaginated(Pageable pageable);

    /**
     * Count active screens for a theatre
     * @param theatreId Theatre identifier
     * @return Number of active screens
     */
    @Query("{ 'theatreId': ?0, 'isActive': true, 'deletedAt': null }")
    long countByTheatreIdActive(String theatreId);
}
