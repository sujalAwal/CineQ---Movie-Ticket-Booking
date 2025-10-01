package com.awal.cineq.theater.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.awal.cineq.theater.model.Theater;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    
    List<Theater> findByIsActiveTrue();
    
    List<Theater> findByCity(String city);
    
    List<Theater> findByCityAndIsActiveTrue(String city);
    
    List<Theater> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT DISTINCT t.city FROM Theater t WHERE t.isActive = true ORDER BY t.city")
    List<String> findDistinctCities();
    
    @Query("SELECT t FROM Theater t WHERE t.city = :city AND t.isActive = true ORDER BY t.name")
    List<Theater> findActiveTheatersByCity(@Param("city") String city);
}