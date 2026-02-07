package com.awal.cineq.masterdata.service.impl;

import com.awal.cineq.form.model.FormManager;
import com.awal.cineq.form.model.FormSubmission;
import com.awal.cineq.form.repository.FormManagerRepository;
import com.awal.cineq.form.repository.FormSubmissionRepository;
import com.awal.cineq.masterdata.dto.RoleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.internal.util.Lists;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.awal.cineq.form.enums.FormAction;
import com.awal.cineq.masterdata.dto.PermissionActionDTO;
import com.awal.cineq.masterdata.dto.MasterDataResponse;
import com.awal.cineq.masterdata.service.MasterDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of MasterDataService
 *
 * Responsible for gathering and serving reference data to frontend
 *
 * WHY @Transactional(readOnly=true)?
 * - This is pure read operation (no database writes)
 * - Tells MongoDB/database to use read-only connection (better performance)
 * - No locks needed
 *
 * WHY separate methods?
 * - getAllMasterData() - loads ALL enums (good for page initialization)
 * - getFormActions() - loads ONLY FormAction (lightweight option)
 * - Allows frontend to choose what it needs
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MasterDataServiceImpl implements MasterDataService {

    private final FormSubmissionRepository formSubmissionRepository;
    private final FormManagerRepository formManagerRepository;
    private static final String ROLE_SLUG = "role";
    @Override
    public MasterDataResponse getAllMasterData() {
        log.info("getAllMasterData STARTED");

        try {
            // Get FormAction enum data
            List<PermissionActionDTO> formActions = getFormActionsData();
            List<RoleDTO> roles = getRoleData();

            log.info("getAllMasterData END: formActions count={}", formActions.size());

            // Build response with all available enums
            return MasterDataResponse.builder()
                    .permission(formActions)
                    .role(roles)
                    .additionalData(null)  // Will be populated as more enums are added
                    .build();

        } catch (Exception e) {
            log.error("getAllMasterData ERROR", e);
            throw new RuntimeException("Failed to load master data", e);
        }
    }

    @Override
    public MasterDataResponse getFormActions() {
        log.info("getFormActions STARTED");

        try {
            List<PermissionActionDTO> formActions = getFormActionsData();

            log.info("getFormActions END: count={}", formActions.size());

            // Return response with only FormAction data
            return MasterDataResponse.builder()
                    .permission(formActions)
                    .build();

        } catch (Exception e) {
            log.error("getFormActions ERROR", e);
            throw new RuntimeException("Failed to load form actions", e);
        }
    }

    /**
     * Private helper: Convert FormAction enum to DTOs
     *
     * Converts all FormAction enum values to DTO format
     * for JSON serialization to frontend
     */
    private List<PermissionActionDTO> getFormActionsData() {
        log.debug("Converting FormAction enum to DTOs");

        return Arrays.stream(FormAction.values())
                .map(PermissionActionDTO::fromEnum)
                .collect(Collectors.toList());
    }

    private List<RoleDTO> getRoleData() {
        log.debug("Converting Role enum to DTOs");

        Optional<FormManager> formManager =  formManagerRepository.findBySlug(ROLE_SLUG);
        if (! formManager.isPresent()) {

            return new ArrayList<>();
        }
        String formId = formManager.get().getId();
            // Fetch roles from form submissions
        PageRequest pageRequest = PageRequest.of( 0, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<FormSubmission> roles = formSubmissionRepository.findSubmittedDataWithNameAndIsActive(formId, pageRequest);

        List<RoleDTO> roleDTOs = new ArrayList();
        roles.forEach(role -> {

            RoleDTO roleDTO = new RoleDTO();
            roleDTO.setName(role.getFormData().get("name").toString());
            roleDTO.setIsActive(role.getIsActive());
            roleDTOs.add(roleDTO);
        });
        return roleDTOs;
    }
}

