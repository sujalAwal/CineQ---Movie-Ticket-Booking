package com.awal.cineq.email.repository;

import com.awal.cineq.email.model.EmailTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends MongoRepository<EmailTemplate, String> {

    /**
     * Find active email template by slug (isActive=true and deletedAt=null)
     */
    Optional<EmailTemplate> findBySlugAndIsActiveTrueAndDeletedAtNull(String slug);

    /**
     * Find any email template by slug (active or inactive, but not deleted)
     */
    Optional<EmailTemplate> findBySlugAndDeletedAtNull(String slug);
}
