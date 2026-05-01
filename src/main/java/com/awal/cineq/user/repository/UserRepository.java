package com.awal.cineq.user.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.awal.cineq.user.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndIsActiveTrue(String email);
    
    List<User> findByIsActiveTrue();
    
    /**
     * Find users by roleId (reference to the roles collection).
     *
     * @param roleId The ObjectId of the role document in the roles collection
     * @return List of users with the specified roleId
     */
    List<User> findByRoleId(String roleId);

    // MongoDB query: uses regex for case-insensitive pattern matching (like SQL LIKE)
    // $or: checks if name OR email matches the keyword
    // $regex: MongoDB's equivalent to SQL LIKE %keyword%
    // Options 'i' = case-insensitive
    @Query("{ '$and': [ " +
           "  { '$or': [ " +
           "    { 'name': { '$regex': ?0, '$options': 'i' } }, " +
           "    { 'email': { '$regex': ?0, '$options': 'i' } } " +
           "  ] }, " +
           "  { 'isActive': true } " +
           "] }")
    List<User> searchUsers(String keyword);

    boolean existsByEmail(String email);
    
    /**
     * Count users by roleId (reference to the roles collection).
     *
     * @param roleId The ObjectId of the role document in the roles collection
     * @return Count of active users with the specified roleId
     */
    @Query(value = "{ 'role_id': ?0, 'isActive': true }", count = true)
    Long countByRoleId(String roleId);
}