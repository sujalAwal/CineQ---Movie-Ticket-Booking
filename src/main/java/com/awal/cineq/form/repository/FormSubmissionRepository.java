package com.awal.cineq.form.repository;

import com.awal.cineq.form.model.FormSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for FormSubmission
 */
@Repository
public interface FormSubmissionRepository extends MongoRepository<FormSubmission, String> {

    // Find submissions by form manager id (exclude soft-deleted)
    @Query("{ 'formManagerId': ?0, 'deletedAt': null }")
    Page<FormSubmission> findByFormManagerId(String formManagerId, Pageable pageable);

    // Find submissions by form step id (exclude soft-deleted)
    @Query("{ 'formStepId': ?0, 'deletedAt': null }")
    Page<FormSubmission> findByFormStepId(String formStepId, Pageable pageable);

    // Find submissions by submitted by user (exclude soft-deleted)
    @Query("{ 'submittedBy': ?0, 'deletedAt': null }")
    List<FormSubmission> findBySubmittedBy(String submittedBy);

    // Find submission by form manager id and submission id (exclude soft-deleted)
    @Query("{ 'formManagerId': ?0, '_id': ?1, 'deletedAt': null }")
    Optional<FormSubmission> findByFormIdAndChildId(String formManagerId, String childId);

    /**
     * Get specific keys from submittedData
     */
    @Aggregation(pipeline = {
            "{ '$match': { 'formManagerId': ?0, 'deletedAt': null ,'isActive': true} }",
            "{ '$project': { 'submittedData': { 'name': 1 }, '_id': 1 } }"
    })
    List<FormSubmission> findSubmittedDataWithNameAndIsActive(String formManagerId, Pageable pageable);
}

