package com.awal.cineq.theater.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.awal.cineq.theater.model.Theater;

import java.util.List;

/**
 * MongoDB Repository for Theater
 * Includes soft-delete queries (deletedAt = null)
 */
@Repository
public interface TheaterRepository extends MongoRepository<Theater, String> {

    @Query("{ 'isActive': true, 'deletedAt': null }")
    List<Theater> findByIsActiveTrue();
    
    @Query("{ 'city': ?0, 'deletedAt': null }")
    List<Theater> findByCity(String city);
    
    @Query("{ 'city': ?0, 'isActive': true, 'deletedAt': null }")
    List<Theater> findByCityAndIsActiveTrue(String city);
    
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'deletedAt': null }")
    List<Theater> findByNameContainingIgnoreCase(String name);
    
    @Query(value = "{ 'isActive': true, 'deletedAt': null }", fields = "{ 'city': 1 }")
    List<Theater> findDistinctCitiesRaw();

    @Query(value = "{ 'city': ?0, 'isActive': true, 'deletedAt': null }", sort = "{ 'name': 1 }")
    List<Theater> findActiveTheatersByCity(String city);
}