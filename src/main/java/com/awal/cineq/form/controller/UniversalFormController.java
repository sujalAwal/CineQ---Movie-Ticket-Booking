package com.awal.cineq.form.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.form.dto.request.BulkDeleteRequest;
import com.awal.cineq.form.dto.request.BulkStatusUpdateRequest;
import com.awal.cineq.form.dto.request.DynamicFormRequest;
import com.awal.cineq.form.dto.response.BulkDeleteResponse;
import com.awal.cineq.form.dto.response.BulkStatusUpdateResponse;
import com.awal.cineq.form.dto.response.FormSubmissionResponse;
import com.awal.cineq.form.service.UniversalFormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for universal dynamic form submission
 * All IDs are MongoDB ObjectIds stored as String
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class UniversalFormController {

    private final UniversalFormService universalFormService;

    private static final String DEFAULT_PAGE = "1";
    private static final String DEFAULT_SIZE = "15";

    @PostMapping("/submit/{formSlug}")
    public ResponseEntity<ApiResponse<FormSubmissionResponse>> submitForm(
            @PathVariable String formSlug,
            @Valid @RequestBody DynamicFormRequest dynamicFormRequest,
            HttpServletRequest request) {

        log.info("submitForm STARTED: formSlug={}, stepSlug={}",
                formSlug, dynamicFormRequest.getStepSlug());

        try {
            // Parse user roles from header (comma-separated)
            List<String> userRoles = universalFormService.getUserRoles();

            FormSubmissionResponse submission = universalFormService.submitForm(formSlug, dynamicFormRequest, userRoles);
            ApiResponse<FormSubmissionResponse> response = ApiResponse.success(
                    "Form submitted successfully", submission);
            response.setPath(request.getRequestURI());
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("submitForm ERROR", e);
            throw e;
        }
    }

    /**
     * Get single submission by ID with schema-driven serialization
     *
     * Returns flat serialized object with only fields where serialization.select=true
     * Example response: {success: true, data: {id: "...", name: "...", createdAt: "..."}}
     *
     * @param formSlug The form manager slug
     * @param id The submission ID
     * @param request HTTP request for path tracking
     * @return ApiResponse with serialized flat object
     */
    @GetMapping("/view/{formSlug}/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubmissionById(
            @PathVariable String formSlug,
            @PathVariable String id,
            HttpServletRequest request) {

        log.info("getSubmissionById STARTED: formSlug={}, id={}", formSlug, id);
        try {
            // Call service method that applies field selection and serialization
            // Returns flat Map<String, Object> with only selected fields
            Map<String, Object> serializedData = universalFormService.getSubmissionByIdWithWorkflow(formSlug, id);

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    "Form submission fetched successfully", serializedData);
            response.setPath(request.getRequestURI());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getSubmissionById ERROR", e);
            throw e;
        }
    }

    /**
     * Get paginated list of submissions by form slug with schema-driven serialization and search filtering
     *
     * Response structure: {success: true, data: [{formSlug: [{...}, {...}]}], page: 1, ...}
     * Example: data: [{"roles": [{id: "...", name: "..."}, {id: "...", name: "..."}]}]
     *
     * @param formSlug The form manager slug
     * @param page Page number (1-based)
     * @param size Page size
     * @param search Optional search term to filter results (case-insensitive, searches configured fields)
     * @return PaginationResponse with data wrapped by formSlug key
     */
    @GetMapping("/list/{formSlug}")
    public PaginationResponse<Map<String, Object>> getSubmissionsByFormSlug(
            @PathVariable String formSlug,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(required = false) String search) {

        log.info("getSubmissionsByFormSlug STARTED: formSlug={}, page={}, size={}, search={}", formSlug, page, size, search);
        try {
            PaginationResponse<Map<String, Object>> response = universalFormService
                    .getSubmissionsByFormSlug(formSlug, page, size, search);
            log.info("getSubmissionsByFormSlug END");
            return response;
        } catch (Exception e) {
            log.error("getSubmissionsByFormSlug ERROR", e);
            throw e;
        }
    }

    /**
     * Get paginated list of submissions by username with schema-driven serialization
     *
     * Response structure: {success: true, data: [{submissions: [{formSlug: "roles", ...}, ...]}], page: 1, ...}
     * Each item includes formSlug for cross-form identification
     *
     * @param username The username who submitted the forms
     * @param page Page number (1-based)
     * @param size Page size
     * @return PaginationResponse with mixed-form submissions
     */
    @GetMapping("/list/user/{username}")
    public PaginationResponse<Map<String, Object>> getSubmissionsByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {

        log.info("getSubmissionsByUser STARTED: username={}, page={}, size={}", username, page, size);
        try {
            PaginationResponse<Map<String, Object>> response = universalFormService
                    .getSubmissionsByUser(username, page, size);
            log.info("getSubmissionsByUser END");
            return response;
        } catch (Exception e) {
            log.error("getSubmissionsByUser ERROR", e);
            throw e;
        }
    }

    /**
     * Bulk update status (isActive) for multiple form submissions
     *
     * Example PATCH request:
     * {
     *   "ids": ["507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012"],
     *   "isActive": true
     * }
     *
     * @param request Contains list of submission IDs and desired isActive status
     * @param servletRequest HTTP request for path tracking
     * @return Response with update summary and detailed results
     */
    @PatchMapping("/update-status")
    public ResponseEntity<ApiResponse<BulkStatusUpdateResponse>> updateSubmissionStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request,
            HttpServletRequest servletRequest) {

        log.info("updateSubmissionStatus STARTED: ids={}, isActive={}", request.getIds(), request.getIsActive());

        try {
            BulkStatusUpdateResponse result = universalFormService.updateBulkStatus(request);

            ApiResponse<BulkStatusUpdateResponse> response = ApiResponse.success(
                    "Submissions updated successfully", result);
            response.setPath(servletRequest.getRequestURI());

            log.info("updateSubmissionStatus END: updated={}, failed={}", result.getUpdated(), result.getFailed());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("updateSubmissionStatus ERROR", e);
            throw e;
        }
    }

    /**
     * Bulk soft-delete documents by IDs
     *
     * Sets deletedAt timestamp for each document instead of hard deleting.
     * Documents are retrieved from targetCollection defined in FormManager's workflowRules.
     *
     * Example DELETE request:
     * {
     *   "formSlug": "roles",
     *   "ids": ["507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012"]
     * }
     *
     * @param request Contains formSlug and list of document IDs to soft-delete
     * @param servletRequest HTTP request for path tracking
     * @return Response with deletion summary and detailed results
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<BulkDeleteResponse>> bulkSoftDelete(
            @Valid @RequestBody BulkDeleteRequest request,
            HttpServletRequest servletRequest) {

        log.info("bulkSoftDelete STARTED: formSlug={}, ids={}", request.getFormSlug(), request.getIds());

        try {
            BulkDeleteResponse result = universalFormService.bulkSoftDelete(request);

            ApiResponse<BulkDeleteResponse> response = ApiResponse.success(
                    "Documents deleted successfully", result);
            response.setPath(servletRequest.getRequestURI());

            log.info("bulkSoftDelete END: deleted={}, failed={}", result.getDeleted(), result.getFailed());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkSoftDelete ERROR", e);
            throw e;
        }
    }
}
