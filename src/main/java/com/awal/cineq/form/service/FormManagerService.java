package com.awal.cineq.form.service;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.form.dto.request.FormManagerRequest;
import com.awal.cineq.form.dto.response.FormManagerResponse;

import java.util.List;

/**
 * Service interface for FormManager operations
 */
public interface FormManagerService {

    PaginationResponse<FormManagerResponse> getAllFormManagers(int page, int size, String sortBy, String sortDirection, String search);

    FormManagerResponse getFormManagerById(String id);

    FormManagerResponse getFormManagerBySlug(String slug);

    FormManagerResponse createFormManager(FormManagerRequest request);

    FormManagerResponse updateFormManager(String id, FormManagerRequest request);

    void deleteFormManager(String id);

    void bulkEnableFormManagers(List<String> ids, boolean enabled);
}
