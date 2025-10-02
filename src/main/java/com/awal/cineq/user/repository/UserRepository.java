package com.awal.cineq.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.awal.cineq.user.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndIsActiveTrue(String email);
    
    List<User> findByIsActiveTrue();
    
    List<User> findByRole(User.UserRole role);
    
    @Query("SELECT u FROM User u WHERE (u.name LIKE %:keyword% OR u.email LIKE %:keyword%) AND u.isActive = true")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = true")
    Long countByRole(@Param("role") User.UserRole role);
}