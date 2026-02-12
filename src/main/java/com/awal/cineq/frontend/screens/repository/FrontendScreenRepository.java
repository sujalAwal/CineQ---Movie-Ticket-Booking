package com.awal.cineq.frontend.screens.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public interface FrontendScreenRepository extends MongoRepository<Map, String> {

  @Query("{ 'isActive': true, 'deletedAt': null }")
  Page<Map> findAllActive(Pageable pageable);

  @Query("{ 'isActive': true, 'deletedAt': null, '$or': [{ 'screenName': { '$regex': ?0, '$options': 'i' } }, { 'screenType': { '$regex': ?0, '$options': 'i' } }] }")
  Page<Map> searchByNameOrType(String searchTerm, Pageable pageable);

  @Query("{ '_id': ?0, 'isActive': true, 'deletedAt': null }")
  Map findByIdAndActive(String id);

  @Query("{ 'theatreId': ?0, 'isActive': true, 'deletedAt': null }")
  Page<Map> findByTheatreId(String theatreId, Pageable pageable);

  @Query("{ 'screenType': ?0, 'isActive': true, 'deletedAt': null }")
  Page<Map> findByScreenType(String screenType, Pageable pageable);
}
