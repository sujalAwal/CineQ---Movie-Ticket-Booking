package com.awal.cineq.user.repository;

import com.awal.cineq.user.model.UserHasRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserHasRoleRepository extends MongoRepository<UserHasRole, String> {

    @Query("{ 'user_id': ?0, 'is_active': true, 'deleted_at': null }")
    List<UserHasRole> findByUserIdActive(String userId);

    @Query("{ 'user_id': ?0, 'role_id': ?1, 'deleted_at': null }")
    Optional<UserHasRole> findByUserIdAndRoleId(String userId, String roleId);

    @Query("{ 'user_id': ?0, 'deleted_at': null }")
    Page<UserHasRole> findAllByUserId(String userId, Pageable pageable);

    @Query("{ 'user_id': ?0, 'deleted_at': null }")
    List<UserHasRole> findAllByUserIdIncludingInactive(String userId);

    @Query("{ 'user_id': ?0, 'is_active': true, 'deleted_at': null }")
    List<UserHasRole> findActiveRolesByUserId(String userId);

    @Query("{ 'role_id': ?0, 'is_active': true, 'deleted_at': null }")
    List<UserHasRole> findByRoleIdActive(String roleId);

    @Query("{ 'user_id': ?0 }")
    void deleteByUserId(String userId);
}
