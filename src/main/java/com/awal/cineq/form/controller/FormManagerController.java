package com.awal.cineq.form.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.form.dto.request.BulkFormManagerStatusRequest;
import com.awal.cineq.form.dto.request.FormManagerRequest;
import com.awal.cineq.form.dto.response.FormManagerResponse;
import com.awal.cineq.form.service.FormManagerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for FormManager endpoints
 * All IDs are MongoDB ObjectIds stored as String
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/form-manager")
public class FormManagerController {

    private final FormManagerService formManagerService;

    private static final String DEFAULT_PAGE = "1";
    private static final String DEFAULT_SIZE = "15";
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    @GetMapping(path = {"", "/"})
    public PaginationResponse<FormManagerResponse> getAllFormManagers(
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIRECTION) String sortDirection,
            @RequestParam(required = false) String search) {

        log.info("getAllFormManagers STARTED: page={}, size={}, sortBy={}, sortDirection={}, search={}", 
                 page, size, sortBy, sortDirection, search);
        try {
            PaginationResponse<FormManagerResponse> response = formManagerService
                    .getAllFormManagers(page, size, sortBy, sortDirection, search);
            log.info("getAllFormManagers END");
            return response;
        } catch (Exception e) {
            log.error("getAllFormManagers ERROR", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FormManagerResponse>> getFormManagerById(
            @PathVariable String id, 
            HttpServletRequest request) {
        
        log.info("getFormManagerById STARTED: id={}", id);
        try {
            FormManagerResponse formManager = formManagerService.getFormManagerById(id);
            ApiResponse<FormManagerResponse> response = ApiResponse.success(
                    "Form manager fetched successfully", formManager);
            response.setPath(request.getRequestURI());
            log.info("getFormManagerById END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getFormManagerById ERROR", e);
            throw e;
        }
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<FormManagerResponse>> getFormManagerBySlug(
            @PathVariable String slug, 
            HttpServletRequest request) {
        
        log.info("getFormManagerBySlug STARTED: slug={}", slug);
        try {
            FormManagerResponse formManager = formManagerService.getFormManagerBySlug(slug);
            ApiResponse<FormManagerResponse> response = ApiResponse.success(
                    "Form manager fetched successfully", formManager);
            response.setPath(request.getRequestURI());
            log.info("getFormManagerBySlug END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getFormManagerBySlug ERROR", e);
            throw e;
        }
    }

    @PostMapping(path = {"", "/"})
    public ResponseEntity<ApiResponse<FormManagerResponse>> createFormManager(
            @Valid @RequestBody FormManagerRequest formManagerRequest,
            HttpServletRequest request) {
        
        log.info("createFormManager STARTED");
        log.debug("createFormManager input: {}", formManagerRequest);
        try {
            FormManagerResponse created = formManagerService.createFormManager(formManagerRequest);
            ApiResponse<FormManagerResponse> response = ApiResponse.success(
                    "Form manager created successfully with auto-generated module (code=" + created.getModuleCode() + ")", created);
            response.setPath(request.getRequestURI());
            log.info("createFormManager END: slug={}, moduleCode={}", created.getSlug(), created.getModuleCode());
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("createFormManager ERROR", e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FormManagerResponse>> updateFormManager(
            @PathVariable String id,
            @Valid @RequestBody FormManagerRequest formManagerRequest, 
            HttpServletRequest request) {
        
        log.info("updateFormManager STARTED: id={}", id);
        try {
            FormManagerResponse updated = formManagerService.updateFormManager(id, formManagerRequest);
            ApiResponse<FormManagerResponse> response = ApiResponse.success(
                    "Form manager updated successfully", updated);
            response.setPath(request.getRequestURI());
            log.info("updateFormManager END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("updateFormManager ERROR", e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFormManager(
            @PathVariable String id, 
            HttpServletRequest request) {
        
        log.info("deleteFormManager STARTED: id={}", id);
        try {
            formManagerService.deleteFormManager(id);
            ApiResponse<Void> response = ApiResponse.success("Form manager deleted successfully", null);
            response.setPath(request.getRequestURI());
            log.info("deleteFormManager END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("deleteFormManager ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-enable")
    public ResponseEntity<ApiResponse<Void>> bulkEnableFormManagers(
            @RequestBody BulkFormManagerStatusRequest request, 
            HttpServletRequest httpRequest) {
        
        log.info("bulkEnableFormManagers STARTED");
        try {
            formManagerService.bulkEnableFormManagers(request.getIds(), true);
            ApiResponse<Void> response = ApiResponse.success("Form managers enabled successfully", null);
            response.setPath(httpRequest.getRequestURI());
            log.info("bulkEnableFormManagers END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkEnableFormManagers ERROR", e);
            throw e;
        }
    }

    @PostMapping("/bulk-disable")
    public ResponseEntity<ApiResponse<Void>> bulkDisableFormManagers(
            @RequestBody BulkFormManagerStatusRequest request, 
            HttpServletRequest httpRequest) {
        
        log.info("bulkDisableFormManagers STARTED");
        try {
            formManagerService.bulkEnableFormManagers(request.getIds(), false);
            ApiResponse<Void> response = ApiResponse.success("Form managers disabled successfully", null);
            response.setPath(httpRequest.getRequestURI());
            log.info("bulkDisableFormManagers END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("bulkDisableFormManagers ERROR", e);
            throw e;
        }
    }
}
