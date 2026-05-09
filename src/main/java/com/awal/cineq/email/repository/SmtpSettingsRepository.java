package com.awal.cineq.email.repository;

import com.awal.cineq.email.model.SmtpSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmtpSettingsRepository extends MongoRepository<SmtpSettings, String> {

    @Query("{ 'is_active': true, 'deleted_at': null }")
    Optional<SmtpSettings> findActiveSmtpSettings();

    @Query("{ 'is_active': true, 'deleted_at': null }")
    List<SmtpSettings> findAllActiveSmtpSettings();
}
