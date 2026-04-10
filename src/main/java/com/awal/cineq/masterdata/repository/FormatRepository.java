package com.awal.cineq.masterdata.repository;

import com.awal.cineq.masterdata.model.Format;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Format Reference Data
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface FormatRepository extends MongoRepository<Format, String> {

    // Find all active formats (soft-delete filter)
    @Query("{ $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ], 'isActive': true }")
    List<Format> findAllActive();

    // Find by code with soft-delete filter
    @Query("{ 'code': ?0, $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ] }")
    Optional<Format> findByCodeActive(String code);
}
