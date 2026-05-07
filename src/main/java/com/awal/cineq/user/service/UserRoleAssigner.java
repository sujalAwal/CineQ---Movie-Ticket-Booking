package com.awal.cineq.user.service;

import com.awal.cineq.exception.BadRequestException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.user.model.UserHasRole;
import com.awal.cineq.user.repository.UserHasRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates role IDs against the roles collection and syncs {@code user_has_roles} for a user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleAssigner {

    private final UserHasRoleRepository userHasRoleRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Fetches the role name from the roles collection by roleId.
     */
    public String fetchRoleNameById(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            log.warn("fetchRoleNameById: roleId is null or blank, returning default role 'USER'");
            return "USER";
        }

        try {
            Query roleQuery = Query.query(
                    Criteria.where("_id").is(new ObjectId(roleId))
                            .and("isActive").is(true)
                            .and("deletedAt").is(null)
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> roleDoc = mongoTemplate.findOne(roleQuery, Map.class, "roles");

            if (roleDoc != null && roleDoc.get("name") != null) {
                String roleName = roleDoc.get("name").toString();
                log.debug("fetchRoleNameById: Found role - id={}, name={}", roleId, roleName);
                return roleName;
            }
            log.warn("fetchRoleNameById: Role not found for roleId={}, returning default 'USER'", roleId);
            return "USER";
        } catch (IllegalArgumentException e) {
            log.warn("fetchRoleNameById: Invalid ObjectId format for roleId={}, returning default 'USER'", roleId);
            return "USER";
        }
    }

    /**
     * Validates that all role IDs exist and are not deleted; returns id → name in request order (deduped).
     */
    public LinkedHashMap<String, String> validateAndFetchRoleNames(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }

        LinkedHashSet<String> uniqueOrdered = new LinkedHashSet<>();
        for (String id : roleIds) {
            if (id != null && !id.isBlank()) {
                uniqueOrdered.add(id.trim());
            }
        }
        if (uniqueOrdered.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }

        List<ObjectId> objectIds = new ArrayList<>();
        for (String id : uniqueOrdered) {
            try {
                objectIds.add(new ObjectId(id));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role ID format: " + id);
            }
        }

        Query roleQuery = Query.query(
                Criteria.where("_id").in(objectIds).and("isActive").is(true).and("deletedAt").is(null)
        );

        List<Map> roleDocs = mongoTemplate.find(roleQuery, Map.class, "roles");

        if (roleDocs.size() != uniqueOrdered.size()) {
            throw new ResourceNotFoundException("One or more roles were not found or are inactive");
        }

        Map<String, String> idToName = new LinkedHashMap<>();
        for (Map doc : roleDocs) {
            String id = doc.get("_id").toString();
            String name = doc.get("name") != null ? doc.get("name").toString() : "USER";
            idToName.put(id, name);
        }

        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        for (String id : uniqueOrdered) {
            ordered.put(id, idToName.get(id));
        }
        return ordered;
    }

    /**
     * Replaces the user's active role assignments with the given role IDs (validated against roles collection).
     */
    public void replaceUserRoles(String userId, List<String> roleIds) {
        LinkedHashMap<String, String> idToName = validateAndFetchRoleNames(roleIds);
        List<String> orderedIds = new ArrayList<>(idToName.keySet());
        Set<String> wanted = new LinkedHashSet<>(orderedIds);

        List<UserHasRole> activeRows = userHasRoleRepository.findByUserIdActive(userId);
        for (UserHasRole ur : activeRows) {
            if (!wanted.contains(ur.getRoleId())) {
                ur.softDelete();
                userHasRoleRepository.save(ur);
            }
        }

        List<UserHasRole> afterRemovals = userHasRoleRepository.findByUserIdActive(userId);
        Set<String> alreadyHave = afterRemovals.stream()
                .map(UserHasRole::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String roleId : orderedIds) {
            if (!alreadyHave.contains(roleId)) {
                UserHasRole uhr = new UserHasRole();
                uhr.setUserId(userId);
                uhr.setRoleId(roleId);
                uhr.setRoleName(idToName.get(roleId));
                uhr.setIsActive(true);
                userHasRoleRepository.save(uhr);
                log.debug("replaceUserRoles: inserted userId={}, roleId={}", userId, roleId);
            }
        }
    }
}
