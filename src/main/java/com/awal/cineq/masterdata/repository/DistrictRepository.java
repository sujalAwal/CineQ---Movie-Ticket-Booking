package com.awal.cineq.masterdata.repository;

import com.awal.cineq.masterdata.model.District;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for District Reference Data
 * Includes soft-delete queries (deletedAt = null)
 * Results can be filtered by activeForCustomerPortal flag
 */
@Repository
public interface DistrictRepository extends MongoRepository<District, String> {

    // Find all active districts
    @Query("{ $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ], 'isActive': true }")
    List<District> findAllActive();

    // Find by code with soft-delete filter
    @Query("{ 'code': ?0, $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ] }")
    Optional<District> findByCodeActive(String code);

    // Find districts by province with soft-delete filter
    @Query("{ 'province_id': ?0, $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ], 'isActive': true }")
    List<District> findByProvinceIdActive(String provinceId);

    // Find districts active in customer portal
    @Query("{ $or: [ { 'deletedAt': null }, { 'deletedAt': { $exists: false } } ], 'isActive': true, 'activeForCustomerPortal': true }")
    List<District> findAllActiveForCustomerPortal();
}
