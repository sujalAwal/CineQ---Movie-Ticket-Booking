package com.awal.cineq.form.service;

import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.form.model.FormManager;
import com.awal.cineq.form.model.FormStep;
import com.awal.cineq.form.repository.FormManagerRepository;
import com.awal.cineq.form.repository.FormStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FormConfigCacheService
 *
 * Purpose: Provide cached access to FormManager and FormStep documents
 *
 * WHY caching?
 * - Form configurations (FormManager, FormStep) are read frequently
 * - They change rarely (only when admin updates them)
 * - Cache reduces MongoDB queries significantly
 * - Perfect for Spring's @Cacheable annotation
 *
 * WHAT is cached?
 * - FormManager by slug (string key) - getFormManagerBySlug()
 * - FormManager by ID - getFormManagerById()
 * - FormStep by ID - getFormStepById()
 * - FormStep by manager ID and slug - getFormStepByManagerIdAndSlug()
 * - List of FormSteps by manager ID - getFormStepsByManagerId()
 *
 * Cache Invalidation:
 * - When FormManager/FormStep is updated/deleted, cache is cleared
 * - Use @CacheEvict on create/update/delete operations
 * - See FormManagerServiceImpl for cache eviction examples
 *
 * Cache Configuration:
 * - Spring Boot auto-configures CaffeineCacheManager (in-memory)
 * - TTL: 1 hour (configured in application.properties)
 * - Max entries: 100 (default)
 *
 * Example:
 * First call: FormManager formManager = cacheService.getFormManagerBySlug("movie-registration")
 *   → Query MongoDB, store in cache, return
 *
 * Second call (within 1 hour): FormManager formManager = cacheService.getFormManagerBySlug("movie-registration")
 *   → Return from cache (no MongoDB query)
 *
 * After update: cache is evicted → Next call queries MongoDB again
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormConfigCacheService {

    private final FormManagerRepository formManagerRepository;
    private final FormStepRepository formStepRepository;
    private final CacheManager cacheManager;

    /**
     * Get FormManager by slug with caching
     * Cache key: "formManager::slug::{slug}"
     *
     * @param slug FormManager slug (e.g., "movie-registration")
     * @return FormManager document
     * @throws ResourceNotFoundException if not found or soft-deleted
     */
    @Cacheable(value = "formManager", key = "'slug:' + #slug")
    public FormManager getFormManagerBySlug(String slug) {
        log.debug("getFormManagerBySlug: Fetching from MongoDB for slug={}", slug);
        return formManagerRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Form manager not found with slug: " + slug));
    }

    /**
     * Get FormManager by ID with caching
     * Cache key: "formManager::id::{id}"
     *
     * @param id FormManager ID
     * @return FormManager document
     * @throws ResourceNotFoundException if not found
     */
    @Cacheable(value = "formManager", key = "'id:' + #id")
    public FormManager getFormManagerById(String id) {
        log.debug("getFormManagerById: Fetching from MongoDB for id={}", id);
        return formManagerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form manager not found with id: " + id));
    }

    /**
     * Get FormStep by ID with caching
     * Cache key: "formStep::id::{id}"
     *
     * @param id FormStep ID
     * @return FormStep document
     * @throws ResourceNotFoundException if not found
     */
    @Cacheable(value = "formStep", key = "'id:' + #id")
    public FormStep getFormStepById(String id) {
        log.debug("getFormStepById: Fetching from MongoDB for id={}", id);
        return formStepRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Form step not found with id: " + id));
    }

    /**
     * Get FormStep by FormManager ID and step slug with caching
     * Cache key: "formStep::manager:{managerId}:slug:{stepSlug}"
     *
     * Note: Returns the OLDEST (first) FormStep if duplicates exist (sorted by createdAt ascending)
     * This handles edge case of duplicate steps due to concurrent creates
     * Duplicates should be cleaned up manually via admin tool
     *
     * @param formManagerId FormManager ID
     * @param stepSlug FormStep slug (e.g., "basic-info", "details")
     * @return FormStep document (oldest by createdAt if duplicates exist)
     * @throws ResourceNotFoundException if not found or soft-deleted
     */
    @Cacheable(value = "formStep", key = "'manager:' + #formManagerId + ':slug:' + #stepSlug")
    public FormStep getFormStepByManagerIdAndSlug(String formManagerId, String stepSlug) {
        log.debug("getFormStepByManagerIdAndSlug: Fetching from MongoDB for managerId={}, stepSlug={}",
                formManagerId, stepSlug);

        List<FormStep> steps = formStepRepository.findByFormManagerIdAndStepSlug(formManagerId, stepSlug);

        if (steps.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Form step not found for managerId: " + formManagerId + ", stepSlug: " + stepSlug);
        }

        // If duplicates exist, log warning and return oldest (first)
        if (steps.size() > 1) {
            log.warn("getFormStepByManagerIdAndSlug: Found {} duplicate steps for managerId={}, stepSlug={}. " +
                    "Returning oldest. Please clean up duplicates manually.",
                    steps.size(), formManagerId, stepSlug);
        }

        return steps.get(0);
    }

    /**
     * Get all FormSteps for a FormManager with caching
     * Cache key: "formStepList::manager:{managerId}"
     *
     * @param formManagerId FormManager ID
     * @return List of FormStep documents, ordered by stepOrder
     */
    @Cacheable(value = "formStepList", key = "'manager:' + #formManagerId")
    public List<FormStep> getFormStepsByManagerId(String formManagerId) {
        log.debug("getFormStepsByManagerId: Fetching from MongoDB for managerId={}", formManagerId);
        return formStepRepository.findByFormManagerIdAndActiveOrderByStepOrder(formManagerId);
    }

    /**
     * Evict all caches for a FormManager
     * Called when FormManager or related FormSteps are updated/deleted
     * Clears all caches related to this form manager
     *
     * @param formManagerId The FormManager ID
     * @param formManagerSlug The FormManager slug
     */
    public void evictAllForFormManager(String formManagerId, String formManagerSlug) {
        log.info("evictAllForFormManager: Clearing cache for managerId={}, slug={}", formManagerId, formManagerSlug);

        // Evict FormManager cache entries
        Cache formManagerCache = cacheManager.getCache("formManager");
        if (formManagerCache != null) {
            formManagerCache.evict("slug:" + formManagerSlug);
            formManagerCache.evict("id:" + formManagerId);
            log.debug("evictAllForFormManager: Evicted formManager cache for slug={}, id={}", formManagerSlug, formManagerId);
        }

        // Evict FormStepList cache for this manager
        Cache formStepListCache = cacheManager.getCache("formStepList");
        if (formStepListCache != null) {
            formStepListCache.evict("manager:" + formManagerId);
            log.debug("evictAllForFormManager: Evicted formStepList cache for managerId={}", formManagerId);
        }

        // Evict all FormStep cache entries (we use allEntries=true approach since step IDs are dynamic)
        // For formStep cache, we need to evict by manager:managerId:slug:* pattern
        // ConcurrentMapCacheManager doesn't support pattern eviction, so we clear all entries
        Cache formStepCache = cacheManager.getCache("formStep");
        if (formStepCache != null) {
            formStepCache.clear();
            log.debug("evictAllForFormManager: Cleared all formStep cache entries");
        }

        log.info("evictAllForFormManager: Cache eviction completed for managerId={}, slug={}", formManagerId, formManagerSlug);
    }
}
