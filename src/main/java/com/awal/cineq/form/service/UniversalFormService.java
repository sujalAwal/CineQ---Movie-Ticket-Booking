package com.awal.cineq.form.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.form.dto.request.BulkStatusUpdateRequest;
import com.awal.cineq.form.dto.request.DynamicFormRequest;
import com.awal.cineq.form.dto.response.BulkStatusUpdateResponse;
import com.awal.cineq.form.dto.response.FormSubmissionResponse;

import java.util.List;
import java.util.Map;

/**
 * Service interface for universal dynamic form handling
 */
public interface UniversalFormService {

    FormSubmissionResponse submitForm(String formSlug, DynamicFormRequest request, List<String> userRoles);

    FormSubmissionResponse getSubmissionById(String id);

    /**
     * Get submission by ID with smart data retrieval based on workflow rules
     *
     * If FormManager has persistToCollection=true AND targetCollection defined:
     *   - Retrieves actual data from target collection using targetCollectionId
     * Else:
     *   - Retrieves from FormSubmission collection
     *
     * Returns serialized, flattened object with only fields where serialization.select=true
     *
     * @param formManagerSlug The form manager slug (e.g., "banners-form", "roles")
     * @param submissionId The submission ID
     * @return Serialized map with selected fields only (e.g., {id: "...", name: "...", createdAt: "..."})
     */
    Map<String, Object> getSubmissionByIdWithWorkflow(String formManagerSlug, String submissionId);

    /**
     * Get paginated submissions by form slug with schema-driven serialization
     *
     * Response structure: {formSlug: [{serializedDoc1}, {serializedDoc2}, ...]}
     * Example: {"roles": [{id: "...", name: "..."}, {id: "...", name: "..."}]}
     *
     * @param formSlug The form manager slug
     * @param page Page number (1-based)
     * @param size Page size
     * @return PaginationResponse with data wrapped by formSlug key
     */
    PaginationResponse<Map<String, Object>> getSubmissionsByFormSlug(String formSlug, int page, int size);

    /**
     * Get paginated submissions by username with schema-driven serialization
     *
     * Response structure: {submissions: [{formSlug: "roles", ...}, {formSlug: "banners", ...}]}
     * Each item includes formSlug for cross-form identification
     *
     * @param username The username who submitted the forms
     * @param page Page number (1-based)
     * @param size Page size
     * @return PaginationResponse with mixed-form submissions
     */
    PaginationResponse<Map<String, Object>> getSubmissionsByUser(String username, int page, int size);

    BulkStatusUpdateResponse updateBulkStatus(BulkStatusUpdateRequest request);

    List<String> getUserRoles();
}
