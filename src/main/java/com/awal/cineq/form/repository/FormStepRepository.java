package com.awal.cineq.form.repository;

import com.awal.cineq.form.model.FormStep;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB Repository for FormStep
 */
@Repository
public interface FormStepRepository extends MongoRepository<FormStep, String> {

    // Find steps by form manager id, ordered by step order (exclude soft-deleted)
    @Query("{ 'formManagerId': ?0, 'deletedAt': null }")
    List<FormStep> findByFormManagerIdAndActiveOrderByStepOrder(String formManagerId);

    // Find step by form manager id and step slug (exclude soft-deleted)
    // Returns List to handle any existing duplicates gracefully (sorted by createdAt, oldest first)
    @Query(value = "{ 'formManagerId': ?0, 'stepSlug': ?1, 'deletedAt': null }", sort = "{ 'createdAt': 1 }")
    List<FormStep> findByFormManagerIdAndStepSlug(String formManagerId, String stepSlug);

    // Find all steps for a form manager (exclude soft-deleted)
    @Query("{ 'formManagerId': ?0, 'deletedAt': null }")
    List<FormStep> findByFormManagerId(String formManagerId);
}
