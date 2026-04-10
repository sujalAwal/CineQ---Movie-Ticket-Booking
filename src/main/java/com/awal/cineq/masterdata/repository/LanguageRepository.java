package com.awal.cineq.masterdata.repository;

import com.awal.cineq.masterdata.model.Language;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for Language Reference Data
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface LanguageRepository extends MongoRepository<Language, String> {

    // Find all active languages (soft-delete filter)
    @Query("{ $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ], 'isActive': true }")
    List<Language> findAllActive();

    // Find by code with soft-delete filter
    @Query("{ 'code': ?0, $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ] }")
    Optional<Language> findByCodeActive(String code);
}
