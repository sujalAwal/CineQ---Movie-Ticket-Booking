package com.awal.cineq.form.service.impl;

import com.awal.cineq.config.ApplicationProperties;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.exception.ValidationException;
import com.awal.cineq.form.dto.request.BulkDeleteRequest;
import com.awal.cineq.form.dto.request.BulkStatusUpdateRequest;
import com.awal.cineq.form.dto.request.DynamicFormRequest;
import com.awal.cineq.form.dto.response.BulkDeleteResponse;
import com.awal.cineq.form.dto.response.BulkStatusUpdateResponse;
import com.awal.cineq.form.dto.response.FormSubmissionResponse;
import com.awal.cineq.form.enums.FormAction;
import com.awal.cineq.form.model.FormManager;
import com.awal.cineq.form.model.FormStep;
import com.awal.cineq.form.model.FormSubmission;
import com.awal.cineq.form.repository.FormSubmissionRepository;
import com.awal.cineq.form.service.FormConfigCacheService;
import com.awal.cineq.form.service.PermissionService;
import com.awal.cineq.form.service.UniversalFormService;
import com.awal.cineq.security.service.RBACPermissionService;
import com.awal.cineq.common.util.FieldTransformer;
import com.awal.cineq.common.util.FieldSelector;
import com.awal.cineq.common.util.FieldSerializer;
import com.awal.cineq.form.validation.JavaAnnotationValidator;
import com.awal.cineq.form.interceptor.InterceptorContext;
import com.awal.cineq.form.interceptor.InterceptorExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MongoDB Implementation of UniversalFormService
 * Handles dynamic form submissions with validation and workflow processing
 * No Hibernate filters needed - MongoDB handles soft-delete via @Query annotations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UniversalFormServiceImpl implements UniversalFormService {

    private final FormConfigCacheService formConfigCacheService;
    private final FormSubmissionRepository formSubmissionRepository;
    private final JavaAnnotationValidator validator;
    private final PermissionService permissionService;
    private final MongoTemplate mongoTemplate;
    private final InterceptorExecutor interceptorExecutor;
    private final RBACPermissionService rbacPermissionService;
    private final ApplicationProperties applicationProperties;

    @Override
    @Transactional
    public FormSubmissionResponse submitForm(String formSlug, DynamicFormRequest request, List<String> userRoles) {
        log.info("submitForm STARTED: formSlug={}, stepSlug={}, action={}, request={}", formSlug, request.getStepSlug(), request.getAction(), request);
        try {
            // Find form manager by slug (cached)
            FormManager formManager = formConfigCacheService.getFormManagerBySlug(formSlug);

            if (!formManager.getIsActive()) {
                throw new BusinessException("Form is not active");
            }

            // Find form step by form manager id and step slug (cached)
            FormStep formStep = formConfigCacheService.getFormStepByManagerIdAndSlug(
                    formManager.getId(), request.getStepSlug());

            if (!formStep.getIsActive()) {
                throw new BusinessException("Form step is not active");
            }

            // Check soft-delete
            if (formStep.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Form step not found");
            }

            // Check permissions based on configured authorization provider
            String authProvider = applicationProperties.getSecurity().getAuthorizationProvider();

            if ("RBAC".equalsIgnoreCase(authProvider)) {
                // Use RBAC-based permission checking
                log.debug("Using RBAC authorization provider");
                checkRBACPermission(formManager.getModuleCode(), request.getAction(), userRoles);
            } else {
                // Use workflow-based permission checking (default)
                log.debug("Using workflow-based authorization provider");
                if (permissionService.hasPermissionsDefined(formStep.getWorkflowRules())) {
                    permissionService.checkPermission(formStep.getWorkflowRules(), request.getAction().toString(), userRoles);
                }
            }

            // Process workflow rules (determine target collection)
            Map<String, Object> workflowRules = formStep.getWorkflowRules();
            String targetCollection = "form_submissions"; // Default collection

            if (workflowRules != null) {
                targetCollection = (String) workflowRules.getOrDefault("targetCollection", "form_submissions");
            }

            // Validate form data with action-aware override logic
            if (formStep.getValidationRules() != null) {
                validator.validate(request.getFormData(), formStep.getValidationRules(),
                        request.getAction().toString(), formManager.getId(), formStep.getId());
            }

            // Build interceptor context (used throughout the method)
            // Get id from formData for UPDATE/DELETE actions
            Object initialDocumentId = request.getFormData() != null ? request.getFormData().get("id") : null;

            InterceptorContext interceptorContext = InterceptorContext.builder()
                    .formData(request.getFormData())
                    .action(request.getAction().toString().toLowerCase())
                    .targetCollection(targetCollection)
                    .userRoles(userRoles)
                    .workflowRules(workflowRules)
                    .formManagerId(formManager.getId())
                    .formStepId(formStep.getId())
                    .documentId(initialDocumentId != null ? initialDocumentId.toString() : null)
                    .build();

            // Execute 'before' interceptors (throws exception to stop action)
            interceptorExecutor.executeBefore(interceptorContext);

            log.debug("submitForm: targetCollection={}", targetCollection);

            // Filter and map fields (only save whitelisted fields)
            Map<String, Object> filteredData = filterAndMapFields(
                    request.getFormData(), 
                    formStep.getFormSchema(), 
                    workflowRules);

            // Validate that filteredData is not empty (has at least one field to save)
            if (filteredData.isEmpty()) {
                throw new ValidationException(
                    "No valid fields found to save. Ensure formData matches fieldMapping in workflowRules.");
            }


            // Always add formManagerId and formStepId to stored data
            filteredData.put("formManagerId", formManager.getId());
            filteredData.put("formStepId", formStep.getId());

            // For UPDATE/DELETE, pass through the document ID from raw formData (not in fieldMapping)
            FormAction currentAction = request.getAction();
            if ((currentAction == FormAction.UPDATE || currentAction == FormAction.DELETE)
                    && request.getFormData() != null) {
                Object docId = request.getFormData().get("id");
                if (docId != null) {
                    filteredData.put("id", docId);
                }
            }

            // Track operation success for interceptors
            boolean operationSuccess = false;
            Exception operationException = null;

            try {
                log.debug("submitForm: Persisting data to collection='{}'", targetCollection);

                // Save to target collection (e.g., "banners", "roles", "movies", or "form_submissions")
                Map<String, Object> saveResult = handleDirectSave(
                        targetCollection,
                        request.getAction().toString(),
                        filteredData,
                        workflowRules);

                boolean success = (boolean) saveResult.get("success");
                String message = (String) saveResult.get("message");
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) saveResult.get("data");

                if (success) {
                    String documentId = (String) data.get("id");
                    operationSuccess = true;

                    // Update interceptor context with result
                    interceptorContext.setDocumentId(documentId);
                    interceptorContext.setResult(InterceptorContext.InterceptorResult.builder()
                            .success(true)
                            .message(message)
                            .data(data)
                            .build());

                    log.info("submitForm: ✓ Data persisted to collection '{}' with id: {}",
                            targetCollection, documentId);

                    // Build response with stored data
                    FormSubmissionResponse response = new FormSubmissionResponse();
                    response.setId(documentId);
                    response.setFormData(filteredData);

                    // Execute 'afterReturning' interceptors (only on success)
                    interceptorExecutor.executeAfterReturning(interceptorContext);

                    log.info("submitForm END: documentId={}, action={}, collection={}",
                            documentId, request.getAction(), targetCollection);
                    return response;

                } else {
                    operationSuccess = false;

                    // Update interceptor context with failure result
                    interceptorContext.setResult(InterceptorContext.InterceptorResult.builder()
                            .success(false)
                            .message(message)
                            .data(data)
                            .build());

                    log.error("submitForm: ✗ Failed to persist to collection '{}': {}",
                            targetCollection, message);
                    throw new BusinessException(message);
                }

            } catch (Exception e) {
                operationSuccess = false;

                // Update interceptor context with exception
                interceptorContext.setResult(InterceptorContext.InterceptorResult.builder()
                        .success(false)
                        .message(e.getMessage())
                        .exception(e)
                        .build());

                // Execute 'afterThrowing' interceptors (only on failure)
                interceptorExecutor.executeAfterThrowing(interceptorContext);

                throw e; // Re-throw to be caught by outer catch block
            } finally {
                // Execute 'after' interceptors (always runs, like finally)
                interceptorExecutor.executeAfter(interceptorContext);
            }
        } catch (ResourceNotFoundException | BusinessException e) {
            log.error("submitForm ERROR", e);
            throw e;
        } catch (ValidationException ve) {
            log.error("submitForm VALIDATION ERROR", ve);

            // Extract first error message for toast alert
            Map<String, String> fieldErrors = ve.getFieldErrors();
            String firstErrorMessage = ve.getMessage();

            if (fieldErrors != null && !fieldErrors.isEmpty()) {
                // Get the first error from the map
                firstErrorMessage = fieldErrors.values().iterator().next();
            }

            // Create a new ValidationException with only first error message
            ValidationException cleanException = new ValidationException(firstErrorMessage, fieldErrors);
            throw cleanException;
        } catch (Exception e) {
            log.error("submitForm UNEXPECTED ERROR", e);
            throw new BusinessException("Failed to submit form", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FormSubmissionResponse getSubmissionById(String id) {
        log.info("getSubmissionById STARTED: id={}", id);
        try {
            FormSubmission submission = formSubmissionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Form submission not found with id: " + id));

            // Check soft-delete
            if (submission.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Form submission not found with id: " + id);
            }

            FormSubmissionResponse response = toResponse(submission);
            log.info("getSubmissionById END");
            return response;
        } catch (ResourceNotFoundException e) {
            log.error("getSubmissionById NOT FOUND", e);
            throw e;
        } catch (Exception e) {
            log.error("getSubmissionById ERROR", e);
            throw new BusinessException("Failed to fetch submission by id", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSubmissionByIdWithWorkflow(String formManagerSlug, String submissionId) {
        log.info("getSubmissionByIdWithWorkflow STARTED: formManagerSlug={}, submissionId={}", formManagerSlug, submissionId);
        try {
            // STEP 1: Find FormManager by slug (cached)
            FormManager formManager = formConfigCacheService.getFormManagerBySlug(formManagerSlug);
            log.debug("getSubmissionByIdWithWorkflow: Found FormManager with id={}", formManager.getId());

            // STEP 2: Get FormSteps to retrieve targetCollection and validationRules (cached)
            List<FormStep> formSteps = formConfigCacheService.getFormStepsByManagerId(formManager.getId());
            if (formSteps.isEmpty()) {
                throw new ResourceNotFoundException("No form steps found for form manager id: " + formManager.getId());
            }

            FormStep formStep = formSteps.getFirst();
            Map<String, Object> workflowRules = formStep.getWorkflowRules();
            Map<String, Object> validationRules = formStep.getValidationRules();

            // STEP 3: Get targetCollection from workflowRules
            String targetCollection = "form_submissions"; // Default

            if (workflowRules != null) {
                targetCollection = (String) workflowRules.getOrDefault("targetCollection", "form_submissions");
            }

            log.debug("getSubmissionByIdWithWorkflow: Fetching from collection='{}', id={}",
                    targetCollection, submissionId);

            // STEP 4: Build MongoDB projection from validationRules (PERFORMANCE OPTIMIZATION)
            Map<String, Integer> projectionFields = FieldSelector.buildProjectionFields(validationRules);
            log.debug("getSubmissionByIdWithWorkflow: Built projection with {} fields", projectionFields.size());

            // Convert String ID to ObjectId for MongoDB query
            org.bson.types.ObjectId objectId;
            try {
                objectId = new org.bson.types.ObjectId(submissionId);
            } catch (IllegalArgumentException e) {
                log.warn("getSubmissionByIdWithWorkflow: Invalid ObjectId format: {}", submissionId);
                throw new ResourceNotFoundException("Invalid document ID format: " + submissionId);
            }

            // STEP 5: Query targetCollection by _id with projection
            org.springframework.data.mongodb.core.query.Query query =
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(objectId)
                                    .and("deletedAt").is(null));

            // Apply projection to query (fetch only selected fields from MongoDB)
            FieldSelector.applyProjection(query, projectionFields);

            @SuppressWarnings("unchecked")
            Map<String, Object> documentData = mongoTemplate.findOne(query, Map.class, targetCollection);

            if (documentData == null) {
                log.warn("getSubmissionByIdWithWorkflow: Data not found in '{}' with id={}",
                        targetCollection, submissionId);
                throw new ResourceNotFoundException(
                        "Data not found in collection '" + targetCollection + "' with id: " + submissionId);
            }

            // STEP 6: Serialize document (BSON → JSON, apply outputField mappings)
            Map<String, Object> serializedData = FieldSerializer
                .serializeDocument(documentData, validationRules);

            log.info("getSubmissionByIdWithWorkflow END: found and serialized document from '{}', returned {} fields",
                targetCollection, serializedData.size());
            return serializedData;

        } catch (ResourceNotFoundException e) {
            log.error("getSubmissionByIdWithWorkflow NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getSubmissionByIdWithWorkflow UNEXPECTED ERROR", e);
            throw new BusinessException("Failed to fetch submission with workflow", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Map<String, Object>> getSubmissionsByFormSlug(String formSlug, int page, int size) {
        log.info("getSubmissionsByFormSlug STARTED: formSlug={}, page={}, size={}", formSlug, page, size);
        try {
            // STEP 1: Find form manager by slug (cached)
            FormManager formManager = formConfigCacheService.getFormManagerBySlug(formSlug);

            // STEP 2: Get FormSteps to retrieve targetCollection and validationRules (cached)
            List<FormStep> formSteps = formConfigCacheService.getFormStepsByManagerId(formManager.getId());
            if (formSteps.isEmpty()) {
                throw new ResourceNotFoundException("No form steps found for form manager: " + formSlug);
            }

            FormStep formStep = formSteps.getFirst();
            Map<String, Object> workflowRules = formStep.getWorkflowRules();
            Map<String, Object> validationRules = formStep.getValidationRules();

            // STEP 3: Get targetCollection from workflowRules
            String targetCollection = "form_submissions"; // Default

            if (workflowRules != null) {
                targetCollection = (String) workflowRules.getOrDefault("targetCollection", "form_submissions");
            }

            log.debug("getSubmissionsByFormSlug: Querying collection='{}' for formManagerId={}",
                    targetCollection, formManager.getId());

            // STEP 4: Build MongoDB projection from validationRules (PERFORMANCE OPTIMIZATION)
            Map<String, Integer> projectionFields = FieldSelector
                .buildProjectionFields(validationRules);
            log.debug("getSubmissionsByFormSlug: Built projection with {} fields", projectionFields.size());

            // STEP 5: Build query for targetCollection with pagination
            org.springframework.data.mongodb.core.query.Query query =
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria
                                    .where("formManagerId").is(formManager.getId())
                                    .and("deletedAt").is(null));

            // Get total count for pagination (without projection - count doesn't need it)
            long totalElements = mongoTemplate.count(query, targetCollection);

            // Apply pagination and sorting
            query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
            query.skip((long) (page - 1) * size);
            query.limit(size);

            // Apply projection to query (fetch only selected fields from MongoDB)
            FieldSelector.applyProjection(query, projectionFields);

            // STEP 6: Execute query
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) (List<?>)
                    mongoTemplate.find(query, Map.class, targetCollection);

            // STEP 7: Serialize each document (BSON → JSON, apply outputField mappings)
            List<Map<String, Object>> serializedDocuments = documents.stream()
                    .map(doc -> FieldSerializer.serializeDocument(doc, validationRules))
                    .collect(Collectors.toList());

            // STEP 8: Wrap serialized data with formSlug key
            // Structure: {formSlug: [{...}, {...}]}
            Map<String, Object> wrappedData = new HashMap<>();
            wrappedData.put(formSlug, serializedDocuments);

            // Convert to list with single wrapped map for PaginationResponse
            List<Map<String, Object>> responseData = List.of(wrappedData);

            // Calculate pagination info
            int totalPages = (int) Math.ceil((double) totalElements / size);
            boolean hasNext = page < totalPages;
            boolean hasPrevious = page > 1;

            log.info("getSubmissionsByFormSlug END: found {} documents in '{}', serialized and wrapped with key '{}'",
                serializedDocuments.size(), targetCollection, formSlug);

            return PaginationResponse.success(
                    "Form submissions fetched successfully",
                    responseData,
                    page,
                    size,
                    totalPages,
                    totalElements,
                    hasNext,
                    hasPrevious
            );
        } catch (ResourceNotFoundException e) {
            log.error("getSubmissionsByFormSlug NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getSubmissionsByFormSlug ERROR", e);
            throw new BusinessException("Failed to fetch submissions by form slug", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Map<String, Object>> getSubmissionsByUser(String username, int page, int size) {
        log.info("getSubmissionsByUser STARTED: username={}, page={}, size={}", username, page, size);
        try {
            // STEP 1: Fetch FormSubmissions from form_submissions collection to get metadata
            // This contains formManagerId and formStepId needed for serialization
            List<FormSubmission> submissions = formSubmissionRepository.findBySubmittedBy(username);

            if (submissions.isEmpty()) {
                log.info("getSubmissionsByUser: No submissions found for user '{}'", username);
                return PaginationResponse.success(
                    "User submissions fetched successfully",
                    List.of(Map.of("submissions", List.of())),
                    page,
                    size,
                    0,
                    0L,
                    false,
                    false
                );
            }

            // STEP 2: Apply pagination to submission list
            int start = (page - 1) * size;
            int end = Math.min(start + size, submissions.size());
            List<FormSubmission> paginatedSubmissions = submissions.subList(start, end);

            // STEP 3: For each submission, fetch from target collection with serialization
            List<Map<String, Object>> serializedSubmissions = new ArrayList<>();

            for (FormSubmission submission : paginatedSubmissions) {
                try {
                    // Get FormManager to retrieve formSlug and targetCollection
                    FormManager formManager = formConfigCacheService.getFormManagerById(submission.getFormManagerId());
                    String formSlug = formManager.getSlug();

                    // Get FormStep for validationRules and workflowRules
                    FormStep formStep = formConfigCacheService.getFormStepById(submission.getFormStepId());
                    Map<String, Object> validationRules = formStep.getValidationRules();
                    Map<String, Object> workflowRules = formStep.getWorkflowRules();

                    // Determine target collection
                    String targetCollection = "form_submissions";
                    if (workflowRules != null) {
                        targetCollection = (String) workflowRules.getOrDefault("targetCollection", "form_submissions");
                    }

                    // Build projection
                    Map<String, Integer> projectionFields = FieldSelector
                        .buildProjectionFields(validationRules);

                    // Query target collection
                    org.bson.types.ObjectId objectId = new org.bson.types.ObjectId(submission.getId());
                    org.springframework.data.mongodb.core.query.Query query =
                        new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(objectId)
                                .and("deletedAt").is(null));

                    // Apply projection
                    FieldSelector.applyProjection(query, projectionFields);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> documentData = mongoTemplate.findOne(query, Map.class, targetCollection);

                    if (documentData != null) {
                        // Serialize document
                        Map<String, Object> serializedDoc = FieldSerializer
                            .serializeDocument(documentData, validationRules);

                        // Add formSlug to identify which form this submission belongs to
                        serializedDoc.put("formSlug", formSlug);

                        serializedSubmissions.add(serializedDoc);
                    } else {
                        log.warn("getSubmissionsByUser: Document not found in '{}' for submission id={}",
                            targetCollection, submission.getId());
                    }

                } catch (Exception e) {
                    log.error("getSubmissionsByUser: Error processing submission id={}: {}",
                        submission.getId(), e.getMessage());
                    // Continue with next submission instead of failing entire request
                }
            }

            // STEP 4: Wrap with "submissions" key for mixed-form results
            Map<String, Object> wrappedData = new HashMap<>();
            wrappedData.put("submissions", serializedSubmissions);

            List<Map<String, Object>> responseData = List.of(wrappedData);

            // Calculate pagination info
            int totalPages = (int) Math.ceil((double) submissions.size() / size);

            log.info("getSubmissionsByUser END: found {} submissions, serialized {} successfully",
                submissions.size(), serializedSubmissions.size());
            return PaginationResponse.success(
                    "User submissions fetched successfully",
                    responseData,
                    page,
                    size,
                    totalPages,
                    (long) submissions.size(),
                    page < totalPages,
                    page > 1
            );
        } catch (Exception e) {
            log.error("getSubmissionsByUser ERROR", e);
            throw new BusinessException("Failed to fetch submissions by user", e);
        }
    }

    private String processWorkflowRules(Map<String, Object> workflowRules, Map<String, Object> formData) {
        if (workflowRules == null || workflowRules.isEmpty()) {
            return "SUBMITTED";
        }

        // Check for workflow actions
        if (workflowRules.containsKey("actions")) {
            @SuppressWarnings("unchecked")
            List<String> actions = (List<String>) workflowRules.get("actions");
            
            for (String action : actions) {
                log.debug("Processing workflow action: {}", action);
            }
        }

        // Check for triggers
        if (workflowRules.containsKey("triggers")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> triggers = (Map<String, Object>) workflowRules.get("triggers");
            
            if (triggers.containsKey("on_complete")) {
                String onComplete = (String) triggers.get("on_complete");
                log.debug("Workflow trigger on_complete: {}", onComplete);
                
                if ("move_to_next".equals(onComplete)) {
                    return "PENDING_NEXT_STEP";
                }
            }
        }

        return "SUBMITTED";
    }

    private FormSubmissionResponse toResponse(FormSubmission submission) {
        return new FormSubmissionResponse(
            submission.getId(),
            submission.getFormManagerId(),
            submission.getFormStepId(),
            submission.getFormData(),
            submission.getIsActive(),
            null  // metadata
        );
    }

    private String getStringFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        } else {
            throw new ValidationException(key + " must be a string value");
        }
    }

    /**
     * Filter and map fields using fieldMapping from workflowRules
     * ONLY fieldMapping is supported - prevents malicious data injection
     * Applies field transformations if defined in workflowRules
     *
     * Returns empty Map if:
     * - fieldMapping is not defined in workflowRules
     * - No submitted fields match fieldMapping keys
     */
    private Map<String, Object> filterAndMapFields(
            Map<String, Object> formData,
            Map<String, Object> formSchema,
            Map<String, Object> workflowRules) {
        
        Map<String, Object> filteredData = new java.util.HashMap<>();
        
        // Single path: ONLY use fieldMapping (no formSchema, no fallback)
        if (workflowRules == null || !workflowRules.containsKey("fieldMapping")) {
            log.warn("fieldMapping not defined in workflowRules - returning empty data (no fields will be saved)");
            return filteredData;  // Return empty Map
        }

        @SuppressWarnings("unchecked")
        Map<String, String> fieldMapping = (Map<String, String>) workflowRules.get("fieldMapping");

        // Loop through fieldMapping and filter submitted data
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            String formField = entry.getKey();           // e.g., "bannerName" (form field)
            String collectionField = entry.getValue();    // e.g., "name" (database field)

            // Only include fields that were actually submitted
            if (formData.containsKey(formField)) {
                Object value = formData.get(formField);

                // Apply field transformers if defined
                value = applyFieldTransformers(formField, value, workflowRules);

                filteredData.put(collectionField, value);
            }
        }
        
        // Check if any fields were successfully mapped
        if (filteredData.isEmpty()) {
            log.warn("No submitted fields matched fieldMapping - returning empty data (no fields will be saved)");
            return filteredData;  // Return empty Map (no data to save)
        }
        
        log.debug("Successfully filtered and mapped {} fields using fieldMapping", filteredData.size());
        return filteredData;  // Return with data (will be saved to DB)
    }

    /**
     * Apply field transformers to a value if fieldTransformers is defined in workflow rules
     *
     * @param fieldName The field name to check for transformer
     * @param value The value to transform
     * @param workflowRules The workflow rules containing fieldTransformers
     * @return Transformed value or original value if no transformer defined
     */
    private Object applyFieldTransformers(String fieldName, Object value, Map<String, Object> workflowRules) {
        // Check if fieldTransformers exist in workflow rules
        if (workflowRules == null || !workflowRules.containsKey("fieldTransformers")) {
            return value;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> fieldTransformers = (Map<String, String>) workflowRules.get("fieldTransformers");

        // Check if this field has a transformer defined
        if (!fieldTransformers.containsKey(fieldName)) {
            return value;
        }

        String transformType = fieldTransformers.get(fieldName);

        // Validate transformation type
        if (!FieldTransformer.isValidTransformType(transformType)) {
            log.warn("Invalid transformation type '{}' for field '{}', returning original value",
                    transformType, fieldName);
            return value;
        }

        try {
            Object transformedValue = FieldTransformer.transformField(value, transformType);
            log.debug("Applied transformation '{}' to field '{}': {} → {}",
                    transformType, fieldName, value, transformedValue);
            return transformedValue;
        } catch (Exception e) {
            log.error("Error applying transformation '{}' to field '{}': {}",
                    transformType, fieldName, e.getMessage());
            return value;  // Return original value on error
        }
    }

    /**
     * Handle direct persistence to target collection
     *
     * Routes requests to CREATE/UPDATE/DELETE handlers based on action.
     * Transforms field names using fieldMapping from workflowRules.
     *
     * DATA FORMAT EXPLANATION:
     * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     * FormSubmission collection stores: Original submitted data (form field names)
     * Target collection stores: Mapped data (collection field names via fieldMapping)
     *
     * Example Flow:
     * 1. User submits: {bannerName: "Summer Sale", bannerImage: "url", status: "active"}
     * 2. fieldMapping: {bannerName: "name", bannerImage: "imageUrl"}
     * 3. FormSubmission stores: {bannerName: "Summer Sale", bannerImage: "url", status: "active"}
     *    (original form field names - audit trail)
     * 4. Target collection stores: {name: "Summer Sale", imageUrl: "url", status: "active",
     *                              createdAt: "...", updatedAt: "...", deletedAt: null}
     *    (mapped field names - collection schema)
     *
     * Note: "status" has no mapping, so it passes through as-is to target collection
     * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     *
     * @param targetCollection The collection name to persist to (e.g., "roles", "banners")
     * @param action The action (CREATE, UPDATE, DELETE)
     * @param filteredData The data after fieldMapping transformation (mapped field names)
     * @param workflowRules The workflow rules containing configuration
     * @return {success: true/false, message: String, data: {id: "...", ...}}
     */
    private Map<String, Object> handleDirectSave(
            String targetCollection,
            String action,
            Map<String, Object> filteredData,
            Map<String, Object> workflowRules) {
        
        log.debug("handleDirectSave: Persisting data to collection '{}' with action '{}'",
                targetCollection, action);

        // Convert string action to enum
        FormAction formAction = FormAction.fromActionName(action);

        if (formAction == null) {
            return Map.of(
                "success", false,
                "message", "Unsupported action: " + action,
                "data", null
            );
        }

        switch (formAction) {
            case CREATE:
                return handleCreate(targetCollection, filteredData, workflowRules);
                
            case UPDATE:
                return handleUpdate(targetCollection, filteredData, workflowRules);
                
            case DELETE:
                return handleDelete(targetCollection, filteredData, workflowRules);
                
            case READ:
            default:
                return Map.of(
                    "success", false,
                    "message", "Action not supported for direct save: " + formAction.getActionName(),
                    "data", null
                );
        }
    }

    /**
     * CREATE: Insert new document into target collection
     * Returns: {success: true, message: "Created", data: {id: "...", targetCollection: "..."}}
     */
    private Map<String, Object> handleCreate(
            String targetCollection,
            Map<String, Object> filteredData,
            Map<String, Object> workflowRules) {
        
        try {
            // Add metadata
            filteredData.put("createdAt", LocalDateTime.now());
            filteredData.put("updatedAt", LocalDateTime.now());
            filteredData.put("deletedAt", null);  // Soft-delete support

            // Execute insert
            Object inserted = mongoTemplate.insert(filteredData, targetCollection);

            // Extract ID from inserted document
            String id = null;
            if (inserted instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = (Map<String, Object>) inserted;
                Object idObj = doc.get("_id");
                id = idObj != null ? idObj.toString() : null;
            }

            log.info("CREATE: Document inserted into '{}' with id: {}", targetCollection, id);

            return Map.of(
                "success", true,
                "message", "Document created successfully",
                "data", Map.of(
                    "id", id,
                    "targetCollection", targetCollection,
                    "action", "create"
                )
            );

        } catch (Exception e) {
            log.error("CREATE: Failed to insert document into '{}': {}", targetCollection, e.getMessage());
            return Map.of(
                "success", false,
                "message", "Failed to create document",
                "data", Map.of(
                    "error", e.getMessage(),
                    "targetCollection", targetCollection,
                    "action", "create"
                )
            );
        }
    }

    /**
     * UPDATE: Update existing document in target collection
     * Returns: {success: true/false, message: String, data: {id: "...", error: "..."}}
     */
    private Map<String, Object> handleUpdate(
            String targetCollection,
            Map<String, Object> filteredData,
            Map<String, Object> workflowRules) {
        
        try {
            // Extract ID from formData (remove to avoid storing as field)
            Object idValue = filteredData.remove("id");

            // VALIDATION: ID must exist for UPDATE action
            if (idValue == null) {
                String customMessage = getCustomActionMessage(workflowRules, "update", "notFound");
                String message = customMessage != null ? customMessage : "ID is required for update action";

                throw new BusinessException(message);
            }

            String id = idValue.toString();

            // Validate ObjectId format using MongoDB's built-in validation
            if (!org.bson.types.ObjectId.isValid(id)) {
                log.warn("UPDATE: Invalid ObjectId format: {}", id);
                String customMessage = getCustomActionMessage(workflowRules, "update", "notFound");
                String message = customMessage != null ? customMessage : "Invalid document ID format";
                return Map.of(
                    "success", false,
                    "message", message,
                    "data", Map.of(
                        "error", "Invalid ObjectId format: " + id,
                        "targetCollection", targetCollection,
                        "action", "update"
                    )
                );
            }

            org.bson.types.ObjectId objectId = new org.bson.types.ObjectId(id);

            // VALIDATION: Check if document exists in target collection
            org.springframework.data.mongodb.core.query.Query existsQuery =
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(objectId));

            boolean exists = mongoTemplate.exists(existsQuery, targetCollection);

            if (!exists) {
                String customMessage = getCustomActionMessage(workflowRules, "update", "notFound");
                String message = customMessage != null ? customMessage : "Document not found";
                String errorMsg = "Document not found with id: " + id + " in collection: " + targetCollection;
                log.warn("UPDATE: {}", errorMsg);

                return Map.of(
                    "success", false,
                    "message", message,
                    "data", Map.of(
                        "error", errorMsg,
                        "id", id,
                        "targetCollection", targetCollection,
                        "action", "update"
                    )
                );
            }

            // Add metadata
            filteredData.put("updatedAt", LocalDateTime.now());

            // Execute update
            org.springframework.data.mongodb.core.query.Update update =
                    new org.springframework.data.mongodb.core.query.Update();

            for (Map.Entry<String, Object> entry : filteredData.entrySet()) {
                update.set(entry.getKey(), entry.getValue());
            }

            var result = mongoTemplate.updateFirst(existsQuery, update, targetCollection);

            log.info("UPDATE: Document updated in '{}' with id: {}", targetCollection, id);

            return Map.of(
                "success", true,
                "message", "Document updated successfully",
                "data", Map.of(
                    "id", id,
                    "targetCollection", targetCollection,
                    "action", "update",
                    "modifiedCount", result.getModifiedCount()
                )
            );

        } catch (Exception e) {
            log.error("UPDATE: Failed to update document: {}", e.getMessage());
            String customMessage = getCustomActionMessage(workflowRules, "update", "notFound");
            String message = customMessage != null ? customMessage : "Failed to update document";

            return Map.of(
                "success", false,
                "message", message,
                "data", Map.of(
                    "error", e.getMessage(),
                    "targetCollection", targetCollection,
                    "action", "update"
                )
            );
        }
    }

    /**
     * DELETE: Soft-delete document in target collection (sets deletedAt = now)
     * Returns: {success: true/false, message: String, data: {id: "...", error: "..."}}
     */
    private Map<String, Object> handleDelete(
            String targetCollection,
            Map<String, Object> filteredData,
            Map<String, Object> workflowRules) {
        
        try {
            // Extract ID from formData
            Object idValue = filteredData.get("id");
            if (idValue == null) {
                idValue = filteredData.get("_id");
            }

            // VALIDATION: ID must exist for DELETE action
            if (idValue == null) {
                String customMessage = getCustomActionMessage(workflowRules, "delete", "notFound");
                String message = customMessage != null ? customMessage : "ID is required for delete action";

                return Map.of(
                    "success", false,
                    "message", message,
                    "data", Map.of(
                        "error", "ID not found in form data",
                        "targetCollection", targetCollection,
                        "action", "delete"
                    )
                );
            }

            String id = idValue.toString();

            // Validate ObjectId format using MongoDB's built-in validation
            if (!org.bson.types.ObjectId.isValid(id)) {
                log.warn("DELETE: Invalid ObjectId format: {}", id);
                String customMessage = getCustomActionMessage(workflowRules, "delete", "notFound");
                String message = customMessage != null ? customMessage : "Invalid document ID format";
                return Map.of(
                    "success", false,
                    "message", message,
                    "data", Map.of(
                        "error", "Invalid ObjectId format: " + id,
                        "targetCollection", targetCollection,
                        "action", "delete"
                    )
                );
            }

            org.bson.types.ObjectId objectId = new org.bson.types.ObjectId(id);

            // VALIDATION: Check if document exists in target collection
            org.springframework.data.mongodb.core.query.Query existsQuery =
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(objectId));

            boolean exists = mongoTemplate.exists(existsQuery, targetCollection);

            if (!exists) {
                String customMessage = getCustomActionMessage(workflowRules, "delete", "notFound");
                String message = customMessage != null ? customMessage : "Document not found";
                String errorMsg = "Document not found with id: " + id + " in collection: " + targetCollection;
                log.warn("DELETE: {}", errorMsg);

                return Map.of(
                    "success", false,
                    "message", message,
                    "data", Map.of(
                        "error", errorMsg,
                        "id", id,
                        "targetCollection", targetCollection,
                        "action", "delete"
                    )
                );
            }

            // Soft-delete: Set deletedAt = now()
            org.springframework.data.mongodb.core.query.Update update =
                    new org.springframework.data.mongodb.core.query.Update();
            update.set("deletedAt", LocalDateTime.now());

            var result = mongoTemplate.updateFirst(existsQuery, update, targetCollection);

            log.info("DELETE: Document soft-deleted in '{}' with id: {} (set deletedAt={})",
                     targetCollection, id, LocalDateTime.now());

            return Map.of(
                "success", true,
                "message", "Document deleted successfully",
                "data", Map.of(
                    "id", id,
                    "targetCollection", targetCollection,
                    "action", "delete",
                    "modifiedCount", result.getModifiedCount(),
                    "deletedAt", LocalDateTime.now()
                )
            );

        } catch (Exception e) {
            log.error("DELETE: Failed to delete document: {}", e.getMessage());
            String customMessage = getCustomActionMessage(workflowRules, "delete", "notFound");
            String message = customMessage != null ? customMessage : "Failed to delete document";

            return Map.of(
                "success", false,
                "message", message,
                "data", Map.of(
                    "error", e.getMessage(),
                    "targetCollection", targetCollection,
                    "action", "delete"
                )
            );
        }
    }

    /**
     * Extract custom error message from workflowRules.actions[action].messages[errorCode]
     *
     * Format in FormStep:
     * "workflowRules": {
     *   "actions": {
     *     "update": {
     *       "enabled": true,
     *       "messages": {
     *         "notFound": "The banner you're trying to update no longer exists or ID is invalid"
     *       }
     *     },
     *     "delete": {
     *       "enabled": true,
     *       "messages": {
     *         "notFound": "The banner you're trying to delete no longer exists"
     *       }
     *     }
     *   }
     * }
     *
     * Error codes:
     * - "notFound": Used for both missing ID and document not found scenarios
     *
     * @param workflowRules The workflow rules from FormStep
     * @param action The action (update, delete)
     * @param errorCode The error code (notFound)
     * @return Custom message or null if not defined
     */
    private String getCustomActionMessage(Map<String, Object> workflowRules, String action, String errorCode) {
        if (workflowRules == null) {
            return null;
        }

        Object actionsObj = workflowRules.get("actions");
        if (!(actionsObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> actions = (Map<String, Object>) actionsObj;

        Object actionObj = actions.get(action);
        if (!(actionObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> actionConfig = (Map<String, Object>) actionObj;

        Object messagesObj = actionConfig.get("messages");
        if (!(messagesObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> messages = (Map<String, Object>) messagesObj;

        Object message = messages.get(errorCode);
        return message instanceof String ? (String) message : null;
    }

    /**
     * Bulk update isActive status for multiple documents
     * Determines target collection based on form slug (role, banner, etc.)
     * Works with any module collection, not just form_submissions
     *
     * @param request Contains formSlug, list of IDs, and desired isActive status
     * @return Response with update count, failure count, and detailed results
     */
    @Override
    @Transactional
    public BulkStatusUpdateResponse updateBulkStatus(BulkStatusUpdateRequest request) {
        log.info("updateBulkStatus STARTED: formSlug={}, ids={}, isActive={}",
            request.getFormSlug(), request.getIds(), request.getIsActive());

        String formSlug = request.getFormSlug();
        List<String> ids = request.getIds();
        Boolean isActive = request.getIsActive();

        int updated = 0;
        int failed = 0;
        List<FormSubmissionResponse> results = new ArrayList<>();

        try {
            // STEP 1: Find form manager by slug to get targetCollection
            FormManager formManager = formConfigCacheService.getFormManagerBySlug(formSlug);

            if (!formManager.getIsActive()) {
                throw new BusinessException("Form is not active");
            }

            // STEP 2: Get FormStep to retrieve targetCollection from workflowRules
            List<FormStep> formSteps = formConfigCacheService.getFormStepsByManagerId(formManager.getId());
            if (formSteps.isEmpty()) {
                throw new ResourceNotFoundException("No form steps found for form: " + formSlug);
            }

            FormStep formStep = formSteps.getFirst();
            Map<String, Object> workflowRules = formStep.getWorkflowRules();

            // STEP 3: Determine target collection (where documents are stored)
            String targetCollection = "form_submissions";  // Default
            if (workflowRules != null) {
                targetCollection = (String) workflowRules.getOrDefault("targetCollection", "form_submissions");
            }

            log.debug("updateBulkStatus: Using targetCollection='{}' for formSlug='{}'", targetCollection, formSlug);

            // STEP 4: Process each ID
            for (String id : ids) {
                try {
                    log.debug("updateBulkStatus: Processing id={} in collection='{}'", id, targetCollection);

                    // Fetch document by ID from target collection
                    Map<String, Object> document = mongoTemplate.findById(id, Map.class, targetCollection);

                    if (document == null) {
                        log.warn("updateBulkStatus: Document not found with id={} in collection='{}'", id, targetCollection);
                        failed++;
                        continue;
                    }

                    // Check soft-delete
                    Object deletedAt = document.get("deletedAt");
                    if (deletedAt != null) {
                        log.warn("updateBulkStatus: Document {} is soft-deleted in '{}', skipping", id, targetCollection);
                        failed++;
                        continue;
                    }

                    // Update isActive status
                    // For form_submissions, we can use the repository
                    if ("form_submissions".equals(targetCollection)) {
                        FormSubmission submission = formSubmissionRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

                        if (submission.getDeletedAt() != null) {
                            log.warn("updateBulkStatus: Document {} is soft-deleted, skipping", id);
                            failed++;
                            continue;
                        }

                        submission.setIsActive(isActive);
                        submission.setUpdatedAt(LocalDateTime.now());
                        FormSubmission saved = formSubmissionRepository.save(submission);

                        FormSubmissionResponse response = new FormSubmissionResponse();
                        response.setId(saved.getId());
                        response.setIsActive(saved.getIsActive());
                        results.add(response);

                        updated++;
                        log.debug("updateBulkStatus: Successfully updated id={} in 'form_submissions', isActive={}", id, isActive);
                    } else {
                        // For other collections, update via mongoTemplate
                        document.put("isActive", isActive);
                        document.put("updatedAt", LocalDateTime.now());

                        // Save back to collection
                        mongoTemplate.save(document, targetCollection);

                        FormSubmissionResponse response = new FormSubmissionResponse();
                        response.setId(id);
                        response.setIsActive(isActive);
                        results.add(response);

                        updated++;
                        log.debug("updateBulkStatus: Successfully updated id={} in '{}', isActive={}", id, targetCollection, isActive);
                    }

                } catch (Exception e) {
                    log.error("updateBulkStatus: Error updating id={} in '{}': {}", id, targetCollection, e.getMessage(), e);
                    failed++;
                }
            }

            log.info("updateBulkStatus END: formSlug='{}', targetCollection='{}', updated={}, failed={}",
                formSlug, targetCollection, updated, failed);

        } catch (ResourceNotFoundException | BusinessException e) {
            log.error("updateBulkStatus ERROR", e);
            throw e;
        } catch (Exception e) {
            log.error("updateBulkStatus UNEXPECTED ERROR", e);
            throw new BusinessException("Failed to update bulk status", e);
        }

        return BulkStatusUpdateResponse.builder()
                .updated(updated)
                .failed(failed)
                .results(results)
                .build();
    }

    /**
     * Bulk soft-delete documents by IDs
     * Sets deletedAt timestamp for each document (soft-delete pattern)
     *
     * @param request Contains formSlug and list of document IDs to soft-delete
     * @return BulkDeleteResponse with deletion summary and detailed results
     */
    @Override
    @Transactional
    public BulkDeleteResponse bulkSoftDelete(BulkDeleteRequest request) {
        log.info("bulkSoftDelete STARTED: formSlug={}, ids={}",
            request.getFormSlug(), request.getIds());

        String formSlug = request.getFormSlug();
        List<String> ids = request.getIds();

        int deleted = 0;
        int failed = 0;
        List<BulkDeleteResponse.DeleteResult> results = new ArrayList<>();

        try {
            // STEP 1: Find form manager by slug using cache (same as submitForm)
            FormManager formManager = formConfigCacheService.getFormManagerBySlug(formSlug);

            if (!formManager.getIsActive()) {
                throw new BusinessException("Form is not active");
            }

            // STEP 2: Get FormStep to retrieve targetCollection from workflowRules (cached)
            List<FormStep> formSteps = formConfigCacheService.getFormStepsByManagerId(formManager.getId());
            if (formSteps.isEmpty()) {
                throw new ResourceNotFoundException("No form steps found for form: " + formSlug);
            }

            FormStep formStep = formSteps.getFirst();
            Map<String, Object> workflowRules = formStep.getWorkflowRules();

            // STEP 3: Determine target collection (where documents are stored)
            String targetCollection = "form_submissions";  // Default
            if (workflowRules != null) {
                targetCollection = (String) workflowRules.getOrDefault("targetCollection", "form_submissions");
            }

            log.debug("bulkSoftDelete: Using targetCollection='{}' for formSlug='{}'", targetCollection, formSlug);

            // STEP 4: Process each ID for soft-delete
            LocalDateTime deletedAt = LocalDateTime.now();

            for (String id : ids) {
                try {
                    log.debug("bulkSoftDelete: Processing id={} in collection='{}'", id, targetCollection);

                    // Fetch document by ID from target collection
                    Map<String, Object> document = mongoTemplate.findById(id, Map.class, targetCollection);

                    if (document == null) {
                        log.warn("bulkSoftDelete: Document not found with id={} in collection='{}'", id, targetCollection);
                        results.add(BulkDeleteResponse.DeleteResult.builder()
                            .id(id)
                            .success(false)
                            .message("Document not found")
                            .build());
                        failed++;
                        continue;
                    }

                    // Check if already soft-deleted
                    Object existingDeletedAt = document.get("deletedAt");
                    if (existingDeletedAt != null) {
                        log.warn("bulkSoftDelete: Document {} is already soft-deleted in '{}', skipping", id, targetCollection);
                        results.add(BulkDeleteResponse.DeleteResult.builder()
                            .id(id)
                            .success(false)
                            .message("Document already deleted")
                            .build());
                        failed++;
                        continue;
                    }

                    // Perform soft-delete by setting deletedAt timestamp
                    if ("form_submissions".equals(targetCollection)) {
                        // For form_submissions, use the repository
                        FormSubmission submission = formSubmissionRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

                        if (submission.getDeletedAt() != null) {
                            log.warn("bulkSoftDelete: Document {} is already soft-deleted, skipping", id);
                            results.add(BulkDeleteResponse.DeleteResult.builder()
                                .id(id)
                                .success(false)
                                .message("Document already deleted")
                                .build());
                            failed++;
                            continue;
                        }

                        submission.setDeletedAt(deletedAt);
                        submission.setUpdatedAt(deletedAt);
                        formSubmissionRepository.save(submission);

                        results.add(BulkDeleteResponse.DeleteResult.builder()
                            .id(id)
                            .success(true)
                            .message("Deleted successfully")
                            .build());

                        deleted++;
                        log.debug("bulkSoftDelete: Successfully soft-deleted id={} in 'form_submissions'", id);
                    } else {
                        // For other collections, update via mongoTemplate
                        document.put("deletedAt", deletedAt);
                        document.put("updatedAt", deletedAt);

                        // Save back to collection
                        mongoTemplate.save(document, targetCollection);

                        results.add(BulkDeleteResponse.DeleteResult.builder()
                            .id(id)
                            .success(true)
                            .message("Deleted successfully")
                            .build());

                        deleted++;
                        log.debug("bulkSoftDelete: Successfully soft-deleted id={} in '{}'", id, targetCollection);
                    }

                } catch (Exception e) {
                    log.error("bulkSoftDelete: Error soft-deleting id={} in '{}': {}", id, targetCollection, e.getMessage(), e);
                    results.add(BulkDeleteResponse.DeleteResult.builder()
                        .id(id)
                        .success(false)
                        .message("Error: " + e.getMessage())
                        .build());
                    failed++;
                }
            }

            log.info("bulkSoftDelete END: formSlug='{}', targetCollection='{}', deleted={}, failed={}",
                formSlug, targetCollection, deleted, failed);

        } catch (ResourceNotFoundException | BusinessException e) {
            log.error("bulkSoftDelete ERROR", e);
            throw e;
        } catch (Exception e) {
            log.error("bulkSoftDelete UNEXPECTED ERROR", e);
            throw new BusinessException("Failed to bulk soft-delete", e);
        }

        return BulkDeleteResponse.builder()
                .deleted(deleted)
                .failed(failed)
                .results(results)
                .build();
    }

    public List<String> getUserRoles() {
        // 1. Get the current Authentication object from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            // Handle case where no user is authenticated (shouldn't happen
            // if security is properly configured for the endpoint)
            return List.of();
        }

        // 2. Get the authorities (roles) from the Authentication object
        List<String> roles = authentication.getAuthorities().stream()
                // 3. Convert GrantedAuthority objects to a list of role strings
                .map(GrantedAuthority::getAuthority)
                // 2. Filter to keep only the authorities that start with "ROLE_"
                .filter(auth -> auth.startsWith("ROLE_"))

                // 3. Use replaceFirst() to remove only the first occurrence of "ROLE_"
                .map(auth -> auth.replaceFirst("ROLE_", ""))

                // 4. Collect the final list (e.g., "ADMIN")
                .collect(Collectors.toList());

        return roles;
    }

    /**
     * Check RBAC permission using module code and FormAction
     * Called only when AUTHORIZATION_PROVIDER = RBAC
     *
     * @param moduleCode The module code from FormManager
     * @param action The FormAction (CREATE, READ, UPDATE, DELETE)
     * @param userRoles The user's assigned roles
     * @throws BusinessException if user doesn't have permission
     */
    private void checkRBACPermission(Integer moduleCode, FormAction action, List<String> userRoles) {

        // Module code should not be null if we reach here
        if (moduleCode == null) {
            throw new BusinessException("FormManager module_code is not set");
        }

        log.info("checkRBACPermission STARTED: moduleCode={}, action={}, userRoles={}",
                moduleCode, action.getActionName(), userRoles);

        try {
            // Use RBACPermissionService to check permission
            rbacPermissionService.checkPermission(
                userRoles,
                moduleCode,
                java.util.Arrays.asList(action.getCode()),  // Convert FormAction to List<Integer>
                true  // Require all (in this case, just one action)
            );

            log.info("checkRBACPermission SUCCESS: Module {} allows {} action",
                    moduleCode, action.getActionName());

        } catch (BusinessException e) {
            log.error("checkRBACPermission FAILED: {}", e.getMessage());
            throw e;
        }
    }
}
