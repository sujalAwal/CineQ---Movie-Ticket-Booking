package com.awal.cineq.masterdata.service.impl;

import com.awal.cineq.form.model.FormManager;
import com.awal.cineq.form.model.FormSubmission;
import com.awal.cineq.form.repository.FormManagerRepository;
import com.awal.cineq.form.repository.FormSubmissionRepository;
import com.awal.cineq.masterdata.dto.RoleDTO;
import com.awal.cineq.masterdata.model.MovieReleaseStatus;
import com.awal.cineq.masterdata.model.Certification;
import com.awal.cineq.masterdata.model.Language;
import com.awal.cineq.masterdata.model.Format;
import com.awal.cineq.masterdata.repository.MovieReleaseStatusRepository;
import com.awal.cineq.masterdata.repository.CertificationRepository;
import com.awal.cineq.masterdata.repository.LanguageRepository;
import com.awal.cineq.masterdata.repository.FormatRepository;
import com.awal.cineq.masterdata.dto.MovieReleaseStatusDTO;
import com.awal.cineq.masterdata.dto.CertificationDTO;
import com.awal.cineq.masterdata.dto.LanguageDTO;
import com.awal.cineq.masterdata.dto.FormatDTO;
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
    private final MovieReleaseStatusRepository movieReleaseStatusRepository;
    private final CertificationRepository certificationRepository;
    private final LanguageRepository languageRepository;
    private final FormatRepository formatRepository;
    private static final String ROLE_SLUG = "role";
    @Override
    public MasterDataResponse getAllMasterData() {
        log.info("getAllMasterData STARTED");

        try {
            // Get FormAction enum data
            List<PermissionActionDTO> formActions = getFormActionsData();
            List<RoleDTO> roles = getRoleData();
            List<MovieReleaseStatusDTO> movieReleaseStatuses = getMovieReleaseStatusesData();
            List<CertificationDTO> certifications = getCertificationsData();
            List<LanguageDTO> languages = getLanguagesData();
            List<FormatDTO> formats = getFormatsData();

            log.info("getAllMasterData END: formActions count={}, roles count={}, movieReleaseStatuses count={}, certifications count={}, languages count={}, formats count={}",
                    formActions.size(), roles.size(), movieReleaseStatuses.size(), certifications.size(), languages.size(), formats.size());

            // Build response with all available enums
            return MasterDataResponse.builder()
                    .permission(formActions)
                    .role(roles)
                    .movieReleaseStatuses(movieReleaseStatuses)
                    .certifications(certifications)
                    .languages(languages)
                    .formats(formats)
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

    /**
     * Private helper: Fetch Movie Release Statuses
     *
     * Fetches all active movie release statuses where isActive=true and deletedAt=null
     * Converts them to DTO format for JSON serialization to frontend
     */
    private List<MovieReleaseStatusDTO> getMovieReleaseStatusesData() {
        log.debug("Fetching active movie release statuses");

        try {
            List<MovieReleaseStatus> statuses = movieReleaseStatusRepository.findAllActive();
            
            return statuses.stream()
                    .map(status -> MovieReleaseStatusDTO.builder()
                            .id(status.getId())
                            .name(status.getName())
                            .code(status.getCode())
                            .description(status.getDescription())
                            .isActive(status.getIsActive())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching movie release statuses", e);
            return new ArrayList<>();
        }
    }

    /**
     * Private helper: Fetch Certifications
     *
     * Fetches all active certifications where isActive=true and deletedAt=null
     * Converts them to DTO format for JSON serialization to frontend
     */
    private List<CertificationDTO> getCertificationsData() {
        log.debug("Fetching active certifications");

        try {
            List<Certification> certifications = certificationRepository.findAllActive();
            
            return certifications.stream()
                    .map(cert -> CertificationDTO.builder()
                            .id(cert.getId())
                            .name(cert.getName())
                            .code(cert.getCode())
                            .description(cert.getDescription())
                            .isActive(cert.getIsActive())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching certifications", e);
            return new ArrayList<>();
        }
    }

    /**
     * Private helper: Fetch Languages
     *
     * Fetches all active languages where isActive=true and deletedAt=null
     * Converts them to DTO format for JSON serialization to frontend
     */
    private List<LanguageDTO> getLanguagesData() {
        log.debug("Fetching active languages");

        try {
            List<Language> languages = languageRepository.findAllActive();
            
            return languages.stream()
                    .map(lang -> LanguageDTO.builder()
                            .id(lang.getId())
                            .code(lang.getCode())
                            .name(lang.getName())
                            .isActive(lang.getIsActive())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching languages", e);
            return new ArrayList<>();
        }
    }

    /**
     * Private helper: Fetch Formats
     *
     * Fetches all active formats where isActive=true and deletedAt=null
     * Converts them to DTO format for JSON serialization to frontend
     */
    private List<FormatDTO> getFormatsData() {
        log.debug("Fetching active formats");

        try {
            List<Format> formats = formatRepository.findAllActive();
            
            return formats.stream()
                    .map(fmt -> FormatDTO.builder()
                            .id(fmt.getId())
                            .code(fmt.getCode())
                            .name(fmt.getName())
                            .isActive(fmt.getIsActive())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching formats", e);
            return new ArrayList<>();
        }
    }
}

