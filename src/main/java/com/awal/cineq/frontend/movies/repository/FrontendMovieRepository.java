package com.awal.cineq.frontend.movies.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public interface FrontendMovieRepository extends MongoRepository<Map, String> {

  @Query("{ 'isActive': true, 'deletedAt': null }")
  Page<Map> findAllActive(Pageable pageable);

  @Query("{ 'isActive': true, 'deletedAt': null, '$or': [{ 'title': { '$regex': ?0, '$options': 'i' } }, { 'description': { '$regex': ?0, '$options': 'i' } }] }")
  Page<Map> searchByTitleOrDescription(String searchTerm, Pageable pageable);

  @Query("{ '_id': ?0, 'isActive': true, 'deletedAt': null }")
  Map findByIdAndActive(String id);
}
