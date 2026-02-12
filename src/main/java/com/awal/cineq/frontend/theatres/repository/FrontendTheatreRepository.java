package com.awal.cineq.frontend.theatres.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public interface FrontendTheatreRepository extends MongoRepository<Map, String> {

  @Query("{ 'isActive': true, 'deletedAt': null }")
  Page<Map> findAllActive(Pageable pageable);

  @Query("{ 'isActive': true, 'deletedAt': null, '$or': [{ 'name': { '$regex': ?0, '$options': 'i' } }, { 'city': { '$regex': ?0, '$options': 'i' } }, { 'address': { '$regex': ?0, '$options': 'i' } }] }")
  Page<Map> searchByNameOrCityOrAddress(String searchTerm, Pageable pageable);

  @Query("{ '_id': ?0, 'isActive': true, 'deletedAt': null }")
  Map findByIdAndActive(String id);

  @Query("{ 'city': ?0, 'isActive': true, 'deletedAt': null }")
  Page<Map> findByCity(String city, Pageable pageable);

  @Query("{ 'state': ?0, 'isActive': true, 'deletedAt': null }")
  Page<Map> findByState(String state, Pageable pageable);
}
