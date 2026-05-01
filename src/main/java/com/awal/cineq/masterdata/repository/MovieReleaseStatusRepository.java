package com.awal.cineq.masterdata.repository;

import com.awal.cineq.masterdata.model.MovieReleaseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Movie Release Status
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface MovieReleaseStatusRepository extends MongoRepository<MovieReleaseStatus, String> {

    // Find active statuses only (soft-delete filter)
    // Matches documents where deletedAt is null OR field doesn't exist, AND isActive is true
    @Query("{ $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ], 'isActive': true }")
    List<MovieReleaseStatus> findAllActive();

    // Find by ID with soft-delete filter
    @Query("{ '_id': ?0, $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ] }")
    Optional<MovieReleaseStatus> findByIdActive(String id);

    // Find by code with soft-delete filter
    @Query("{ 'code': ?0, $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ] }")
    Optional<MovieReleaseStatus> findByCodeActive(String code);
}
