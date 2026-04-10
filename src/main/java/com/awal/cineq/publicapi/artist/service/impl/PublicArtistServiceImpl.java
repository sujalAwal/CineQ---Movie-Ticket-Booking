package com.awal.cineq.publicapi.artist.service.impl;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.artist.dto.PublicArtistResponse;
import com.awal.cineq.publicapi.artist.service.PublicArtistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicArtistServiceImpl implements PublicArtistService {

    private final MongoTemplate mongoTemplate;

    @Override
    public ApiResponse<List<PublicArtistResponse>> getAllActiveArtists(String artistTypeId) {
        log.info("getAllActiveArtists STARTED: artistTypeId={}", artistTypeId);
        try {
            Criteria criteria = Criteria.where("isActive").is(true).and("deletedAt").is(null);
            
            // Filter by artist type if provided
            if (artistTypeId != null && !artistTypeId.trim().isEmpty()) {
                criteria = criteria.and("artist_type_id").is(artistTypeId);
            }
            
            Query query = new Query(criteria);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> docs = (List<Map<String, Object>>) (List<?>) mongoTemplate.find(query, Map.class, "artists");
            
            List<PublicArtistResponse> artists = docs.stream()
                    .map(this::mapToArtistResponse)
                    .collect(Collectors.toList());
            
            log.info("getAllActiveArtists END: count={}", artists.size());
            return ApiResponse.success("Artists fetched successfully", artists);
        } catch (Exception e) {
            log.error("getAllActiveArtists ERROR", e);
            throw e;
        }
    }

    @Override
    public ApiResponse<List<PublicArtistResponse>> getArtistsByIds(List<String> ids, String artistTypeId) {
        log.info("getArtistsByIds STARTED: ids={}, artistTypeId={}", ids, artistTypeId);
        try {
            if (ids == null || ids.isEmpty()) {
                return ApiResponse.success("No artists found", new ArrayList<>());
            }

            // Convert string IDs to ObjectId
            List<ObjectId> objectIds = ids.stream()
                    .map(id -> {
                        try {
                            return new ObjectId(id);
                        } catch (Exception e) {
                            log.warn("Invalid ObjectId format: {}", id);
                            return null;
                        }
                    })
                    .filter(obj -> obj != null)
                    .collect(Collectors.toList());

            Criteria criteria = Criteria.where("_id").in(objectIds)
                    .and("isActive").is(true)
                    .and("deletedAt").is(null);
            
            // Filter by artist type if provided
            if (artistTypeId != null && !artistTypeId.trim().isEmpty()) {
                criteria = criteria.and("artist_type_id").is(artistTypeId);
            }
            
            Query query = new Query(criteria);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> docs = (List<Map<String, Object>>) (List<?>) mongoTemplate.find(query, Map.class, "artists");
            
            List<PublicArtistResponse> artists = docs.stream()
                    .map(this::mapToArtistResponse)
                    .collect(Collectors.toList());
            
            log.info("getArtistsByIds END: count={}", artists.size());
            return ApiResponse.success("Artists fetched successfully", artists);
        } catch (Exception e) {
            log.error("getArtistsByIds ERROR", e);
            throw e;
        }
    }

    /**
     * Private helper: Map MongoDB document to PublicArtistResponse DTO
     */
    private PublicArtistResponse mapToArtistResponse(Map<String, Object> doc) {
        return PublicArtistResponse.builder()
                .id(doc.get("_id") != null ? doc.get("_id").toString() : null)
                .fullName((String) doc.get("full_name"))
                .bio((String) doc.get("bio"))
                .avatar((String) doc.get("avatar"))
                .birthDate((String) doc.get("birth_date"))
                .rating(doc.get("rating") != null ? ((Number) doc.get("rating")).doubleValue() : null)
                .nationality((String) doc.get("nationality"))
                .moviesCount(doc.get("movies_count") != null ? ((Number) doc.get("movies_count")).intValue() : null)
                .artistTypeId((String) doc.get("artist_type_id"))
                .isActive((Boolean) doc.get("isActive"))
                .build();
    }
}
