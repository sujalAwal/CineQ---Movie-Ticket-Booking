package com.awal.cineq.frontend.showtimes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
public interface FrontendShowtimeRepository extends MongoRepository<Map, String> {

  @Query("{ 'isActive': true, 'deletedAt': null }")
  Page<Map> findAllActive(Pageable pageable);

  @Query("{ 'isActive': true, 'deletedAt': null, '$or': [{ 'movieId': ?0 }, { 'screenId': ?0 }, { 'theatreId': ?0 }] }")
  Page<Map> searchByField(String searchTerm, Pageable pageable);

  @Query("{ '_id': ?0, 'isActive': true, 'deletedAt': null }")
  Map findByIdAndActive(String id);

  @Query("{ 'movieId': ?0, 'isActive': true, 'deletedAt': null }")
  Page<Map> findByMovieId(String movieId, Pageable pageable);

  @Query("{ 'theatreId': ?0, 'isActive': true, 'deletedAt': null }")
  Page<Map> findByTheatreId(String theatreId, Pageable pageable);

  @Query("{ 'screenId': ?0, 'isActive': true, 'deletedAt': null }")
  Page<Map> findByScreenId(String screenId, Pageable pageable);
}
