package com.awal.cineq.rolehasmodule.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.rolehasmodule.dto.RoleHasModuleDTO;
import com.awal.cineq.rolehasmodule.dto.request.RoleHasModulePageRequest;
import com.awal.cineq.rolehasmodule.dto.request.RoleHasModuleRequestDto;
import com.awal.cineq.rolehasmodule.model.RoleHasModule;
import com.awal.cineq.rolehasmodule.repository.RoleHasModuleRepository;
import com.awal.cineq.rolehasmodule.service.RoleHasModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RoleHasModule Service Implementation using MongoDB
 * Handles all business logic for role-module permission management
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoleHasModuleServiceImpl implements RoleHasModuleService {

    private final RoleHasModuleRepository roleHasModuleRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<RoleHasModuleDTO> getRoleHasModules(RoleHasModulePageRequest request) {
        log.info("getRoleHasModules STARTED: request={}", request);
        try {
            PageRequest pageRequest = request.toPageRequest();

            Page<RoleHasModule> roleHasModules = findRoleHasModules(request, pageRequest);
            Long total = roleHasModules.getTotalElements();
            log.debug("Total role-module permissions found: {}", total);

            List<RoleHasModuleDTO> result = roleHasModules.stream()
                    .map(rhm -> modelMapper.map(rhm, RoleHasModuleDTO.class))
                    .toList();

            log.debug("getRoleHasModules result count: {}", result.size());
            log.info("getRoleHasModules END");

            return PaginationResponse.success(
                    "Role-module permissions fetched successfully",
                    result,
                    roleHasModules.getNumber() + 1, // converting to 1-based page index
                    roleHasModules.getSize(),
                    roleHasModules.getTotalPages(),
                    roleHasModules.getTotalElements(),
                    roleHasModules.hasNext(),
                    roleHasModules.hasPrevious()
            );

        } catch (Exception e) {
            log.error("getRoleHasModules ERROR", e);
            throw new BusinessException("Failed to fetch role-module permissions", e);
        }
    }

    @Override
    public RoleHasModuleDTO createRoleHasModule(RoleHasModuleRequestDto requestDto) {
        log.info("createRoleHasModule STARTED: roleId={}, moduleId={}", requestDto.getRoleId(), requestDto.getModuleId());
        try {
            // Check if role-module permission already exists
            if (roleHasModuleRepository.existsByRoleIdAndModuleId(requestDto.getRoleId(), requestDto.getModuleId())) {
                throw new BusinessException("Role-module permission already exists for roleId '" + requestDto.getRoleId()
                    + "' and moduleId '" + requestDto.getModuleId() + "'");
            }

            RoleHasModule roleHasModule = new RoleHasModule();
            roleHasModule.setRoleId(requestDto.getRoleId());
            roleHasModule.setModuleId(requestDto.getModuleId());
            roleHasModule.setRole(requestDto.getRole());
            roleHasModule.setModule(requestDto.getModule());
            roleHasModule.setIsActive(requestDto.is_active());

            RoleHasModule saved = roleHasModuleRepository.save(roleHasModule);
            RoleHasModuleDTO result = modelMapper.map(saved, RoleHasModuleDTO.class);

            log.debug("createRoleHasModule result: {}", result);
            log.info("createRoleHasModule END: id={}", saved.getId());

            return result;
        } catch (BusinessException e) {
            log.error("createRoleHasModule BUSINESS ERROR", e);
            throw e;
        } catch (Exception e) {
            log.error("createRoleHasModule ERROR", e);
            throw new BusinessException("Failed to create role-module permission", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RoleHasModuleDTO getRoleHasModuleById(String id) {
        log.info("getRoleHasModuleById STARTED: id={}", id);
        try {
            RoleHasModule roleHasModule = roleHasModuleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Role-module permission not found with id: " + id));

            // Ensure role-module permission is not soft-deleted
            if (roleHasModule.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Role-module permission not found with id: " + id);
            }

            RoleHasModuleDTO result = modelMapper.map(roleHasModule, RoleHasModuleDTO.class);
            log.debug("getRoleHasModuleById result: {}", result);
            log.info("getRoleHasModuleById END");

            return result;
        } catch (ResourceNotFoundException e) {
            log.error("getRoleHasModuleById NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getRoleHasModuleById ERROR", e);
            throw new BusinessException("Failed to fetch role-module permission by id", e);
        }
    }

    @Override
    public RoleHasModuleDTO updateRoleHasModule(String id, RoleHasModuleRequestDto requestDto) {
        log.info("updateRoleHasModule STARTED: id={}", id);
        try {
            RoleHasModule roleHasModule = roleHasModuleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Role-module permission not found with id: " + id));

            // Ensure role-module permission is not soft-deleted
            if (roleHasModule.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Role-module permission not found with id: " + id);
            }

            roleHasModule.setRoleId(requestDto.getRoleId());
            roleHasModule.setModuleId(requestDto.getModuleId());
            roleHasModule.setRole(requestDto.getRole());
            roleHasModule.setModule(requestDto.getModule());
            roleHasModule.setIsActive(requestDto.is_active());

            RoleHasModule updated = roleHasModuleRepository.save(roleHasModule);
            RoleHasModuleDTO result = modelMapper.map(updated, RoleHasModuleDTO.class);

            log.debug("updateRoleHasModule result: {}", result);
            log.info("updateRoleHasModule END");

            return result;
        } catch (ResourceNotFoundException e) {
            log.error("updateRoleHasModule NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("updateRoleHasModule ERROR", e);
            throw new BusinessException("Failed to update role-module permission", e);
        }
    }

    @Override
    public void deleteRoleHasModule(String id) {
        log.info("deleteRoleHasModule STARTED: id={}", id);
        try {
            RoleHasModule roleHasModule = roleHasModuleRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Role-module permission not found with id: " + id));

            // Ensure role-module permission is not already soft-deleted
            if (roleHasModule.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Role-module permission not found with id: " + id);
            }

            roleHasModule.setDeletedAt(LocalDateTime.now()); // Soft delete
            roleHasModuleRepository.save(roleHasModule);

            log.info("deleteRoleHasModule END: id={}", id);
        } catch (ResourceNotFoundException e) {
            log.error("deleteRoleHasModule NOT FOUND: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("deleteRoleHasModule ERROR", e);
            throw new BusinessException("Failed to delete role-module permission", e);
        }
    }

    @Override
    @Transactional
    public void bulkEnableRoleHasModules(List<String> ids, boolean enabled) {
        log.info("bulkEnableRoleHasModules STARTED: count={}, enabled={}", ids.size(), enabled);
        try {
            List<RoleHasModule> roleHasModules = roleHasModuleRepository.findAllById(ids);

            if (roleHasModules.size() != ids.size()) {
                throw new ResourceNotFoundException("Some role-module permissions not found for the provided IDs");
            }

            // Filter out soft-deleted role-module permissions
            List<RoleHasModule> activeRoleHasModules = roleHasModules.stream()
                    .filter(rhm -> rhm.getDeletedAt() == null)
                    .toList();

            if (activeRoleHasModules.isEmpty()) {
                throw new ResourceNotFoundException("No active role-module permissions found for the provided IDs");
            }

            for (RoleHasModule roleHasModule : activeRoleHasModules) {
                roleHasModule.setIsActive(enabled);
            }

            roleHasModuleRepository.saveAll(activeRoleHasModules);
            log.info("bulkEnableRoleHasModules END: updated={}", activeRoleHasModules.size());
        } catch (Exception e) {
            log.error("bulkEnableRoleHasModules ERROR", e);
            throw new BusinessException("Failed to bulk update role-module permissions", e);
        }
    }

    private Page<RoleHasModule> findRoleHasModules(RoleHasModulePageRequest request, PageRequest pageRequest) {
        log.info("findRoleHasModules STARTED: hasSearch={}, hasRoleFilter={}, hasModuleFilter={}",
            request.hasSearch(), request.hasRoleFilter(), request.hasModuleFilter());

        if (request.hasRoleFilter() && request.hasModuleFilter()) {
            return roleHasModuleRepository.findByRoleAndModuleContainingIgnoreCase(
                request.getRoleId(), request.getModuleId(), pageRequest);
        } else if (request.hasRoleFilter()) {
            return roleHasModuleRepository.findByRoleContainingIgnoreCase(request.getRoleId(), pageRequest);
        } else if (request.hasModuleFilter()) {
            return roleHasModuleRepository.findByModuleContainingIgnoreCase(request.getModuleId(), pageRequest);
        } else if (request.hasSearch()) {
            return roleHasModuleRepository.findByRoleContainingIgnoreCase(request.getSearch(), pageRequest);
        } else {
            return roleHasModuleRepository.findAll(pageRequest);
        }
    }
}
