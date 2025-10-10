package com.awal.cineq.genre.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.awal.cineq.genre.model.Genre;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GenreRepository extends JpaRepository<Genre, UUID> {

    Optional<Genre> findByName(String name);

    Page<Genre> findByNameContainingIgnoreCase(String name, PageRequest pageRequest);
    
    boolean existsByName(String name);
}