package com.awal.cineq.masterdata.repository;

import com.awal.cineq.masterdata.model.Province;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Province Reference Data
 * Includes soft-delete queries (deletedAt = null)
 * Results ordered by 'order' field for UI consistency
 */
@Repository
public interface ProvinceRepository extends MongoRepository<Province, String> {

    // Find all active provinces ordered by 'order' field
    @Query("{ $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ], 'isActive': true }")
    List<Province> findAllActive();

    // Find by code with soft-delete filter
    @Query("{ 'code': ?0, $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ] }")
    Optional<Province> findByCodeActive(String code);
}
