package com.awal.cineq.publicapi.movie.service.impl;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.movies.dto.MovieDTO;
import com.awal.cineq.frontend.movies.dto.request.MoviePageRequest;
import com.awal.cineq.frontend.movies.repository.FrontendMovieRepository;
import com.awal.cineq.genre.repository.GenreRepository;
import com.awal.cineq.masterdata.repository.MovieReleaseStatusRepository;
import com.awal.cineq.publicapi.movie.dto.GenreInfo;
import com.awal.cineq.publicapi.movie.dto.PublicMovieDetailResponse;
import com.awal.cineq.publicapi.movie.dto.StarcastInfo;
import com.awal.cineq.publicapi.movie.dto.ArtistInfo;
import com.awal.cineq.publicapi.movie.dto.ArtistTypeInfo;
import com.awal.cineq.publicapi.movie.service.PublicMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicMovieServiceImpl implements PublicMovieService {

    private final FrontendMovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieReleaseStatusRepository movieReleaseStatusRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public PaginationResponse<MovieDTO> getAllMovies(MoviePageRequest pageRequest) {
        log.info("getAllMovies STARTED: pageRequest={}", pageRequest);
        try {
            // Validate releaseStatus if provided
            if (pageRequest.getReleaseStatus() != null && !pageRequest.getReleaseStatus().isEmpty()) {
                boolean statusExists = movieReleaseStatusRepository
                        .findByCodeActive(pageRequest.getReleaseStatus())
                        .isPresent();
                if (!statusExists) {
                    throw new IllegalArgumentException("Invalid releaseStatus: " + pageRequest.getReleaseStatus());
                }
            }

            // Build query criteria
            Criteria criteria = Criteria.where("isActive").is(true)
                    .and("deletedAt").is(null);

            // Add releaseStatus filter if provided
            // NOTE: Status in DB is stored as UPPERCASE (e.g., 'COMING_SOON', 'NOW_SHOWING')
            if (pageRequest.getReleaseStatus() != null && !pageRequest.getReleaseStatus().isEmpty()) {
                String statusToFilter = pageRequest.getReleaseStatus().toUpperCase();
                criteria = criteria.and("status").is(statusToFilter);
            }

            // Add search filter if provided
            if (pageRequest.getSearch() != null && !pageRequest.getSearch().isEmpty()) {
                String searchRegex = pageRequest.getSearch();
                criteria = criteria.andOperator(
                        new Criteria().orOperator(
                                Criteria.where("title").regex(searchRegex, "i"),
                                Criteria.where("description").regex(searchRegex, "i")
                        )
                );
            }

            // Ensure page and size are valid
            int page = pageRequest.getPage() > 0 ? pageRequest.getPage() : 1;
            int size = pageRequest.getSize() > 0 ? pageRequest.getSize() : 10;

            // Build sort
            Sort sort = Sort.by(Sort.Direction.DESC, "releaseDate");
            if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
                Sort.Direction direction = Sort.Direction.DESC;
                sort = Sort.by(direction, pageRequest.getSortBy());
            }

            // Create pageable
            PageRequest pageable = PageRequest.of(page - 1, size, sort);

            // Execute query
            Query query = new Query(criteria).with(pageable);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> movies = (List<Map<String, Object>>) (List<?>) mongoTemplate.find(query, Map.class, "movies");
            long total = mongoTemplate.count(new Query(criteria), "movies");

            // Convert to DTOs
            List<MovieDTO> dtos = movies.stream()
                    .map(this::mapToMovieDTO)
                    .collect(Collectors.toList());

            // Build paginated response
            Page<MovieDTO> pageResult = PageableExecutionUtils.getPage(
                    dtos,
                    pageable,
                    () -> total
            );

            PaginationResponse<MovieDTO> response = PaginationResponse.success(
                    "Movies retrieved successfully",
                    dtos,
                    page,
                    size,
                    pageResult.getTotalPages(),
                    total,
                    pageResult.hasNext(),
                    pageResult.hasPrevious()
            );
            log.info("getAllMovies END – returned {} movies", dtos.size());
            return response;
        } catch (IllegalArgumentException e) {
            log.error("getAllMovies ERROR – invalid request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("getAllMovies ERROR", e);
            throw e;
        }
    }

    @Override
    public ApiResponse<PublicMovieDetailResponse> getMovieById(String id) {
        log.info("getMovieById STARTED: id={}", id);
        try {
            // Validate ID format
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Movie ID cannot be empty");
            }

            // Check if it's a valid MongoDB ObjectId format (24 hex characters)
            if (!id.matches("^[0-9a-f]{24}$")) {
                throw new IllegalArgumentException("Invalid movie ID format. Expected 24 hex characters.");
            }

            // Query using MongoTemplate with proper ObjectId conversion
            ObjectId objectId = new ObjectId(id);
            Criteria criteria = Criteria.where("_id").is(objectId)
                    .and("isActive").is(true)
                    .and("deletedAt").is(null);
            
            Query query = new Query(criteria);
            Map<String, Object> doc = mongoTemplate.findOne(query, Map.class, "movies");
            
            if (doc == null) {
                throw new ResourceNotFoundException("Movie not found with id: " + id);
            }

            // Resolve genres: raw list of genre ID strings → GenreInfo list
            List<GenreInfo> resolvedGenres = new ArrayList<>();
            Object genresRaw = doc.get("genres");
            if (genresRaw instanceof List<?> genreList) {
                for (Object item : genreList) {
                    if (item instanceof String genreId) {
                        genreRepository.findById(genreId).ifPresent(g ->
                                resolvedGenres.add(new GenreInfo(g.getId(), g.getName()))
                        );
                    }
                }
            }

            // Extract duration safely
            Integer duration = null;
            Object dur = doc.get("duration");
            if (dur instanceof Number) {
                duration = ((Number) dur).intValue();
            }

            // Extract isActive safely
            Boolean isActive = null;
            Object active = doc.get("isActive");
            if (active instanceof Boolean) {
                isActive = (Boolean) active;
            }

            // Extract language safely - it's a List in DB, not a String
            List<String> languages = new ArrayList<>();
            Object langRaw = doc.get("language");
            if (langRaw instanceof List<?> langList) {
                langList.forEach(lang -> {
                    if (lang instanceof String) {
                        languages.add((String) lang);
                    }
                });
            }

            // Enrich starcast data with full artist and artist type information
            List<StarcastInfo> enrichedStarcast = enrichStarcast(doc.get("starcast"));

            PublicMovieDetailResponse detail = PublicMovieDetailResponse.builder()
                    .id(doc.get("_id") != null ? doc.get("_id").toString() : null)
                    .title((String) doc.get("title"))
                    .description((String) doc.get("description"))
                    .poster((String) doc.get("poster"))
                    .banner((String) doc.get("banner"))
                    .trailerUrl((String) doc.get("trailerUrl"))
                    .duration(duration)
                    .releaseDate((String) doc.get("releaseDate"))
                    .language(languages)
                    .country((String) doc.get("country"))
                    .certification((String) doc.get("certification"))
                    .formats(doc.get("formats"))
                    .status((String) doc.get("status"))
                    .director((String) doc.get("director"))
                    .starcast(enrichedStarcast)
                    .genres(resolvedGenres)
                    .isActive(isActive)
                    .build();

            log.info("getMovieById END: id={}", id);
            return ApiResponse.success("Movie fetched successfully", detail);
        } catch (IllegalArgumentException e) {
            log.error("getMovieById ERROR – invalid ID format: id={}, message={}", id, e.getMessage());
            throw e;
        } catch (ResourceNotFoundException e) {
            log.error("getMovieById ERROR – not found: id={}", id);
            throw e;
        } catch (Exception e) {
            log.error("getMovieById ERROR: id={}", id, e);
            throw e;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Maps a raw MongoDB {@link Map} document to a {@link MovieDTO}.
     * Returns essential fields: title, releaseDate, duration, poster, genres, status.
     */
    private MovieDTO mapToMovieDTO(Map<String, Object> doc) {
        MovieDTO dto = new MovieDTO();
        
        // Safely extract and convert ID (BSON ObjectId → String)
        Object idObj = doc.get("_id");
        if (idObj != null) {
            dto.setId(idObj.toString());
        }
        
        dto.setTitle((String) doc.get("title"));
        dto.setPoster((String) doc.get("poster"));
        dto.setStatus((String) doc.get("status"));
        
        Object dur = doc.get("duration");
        if (dur instanceof Number) {
            dto.setDuration(((Number) dur).intValue());
        }
        
        dto.setReleaseDate((String) doc.get("releaseDate"));
        
        // Resolve genres: raw list of genre ID strings → GenreInfo list
        List<GenreInfo> genres = new ArrayList<>();
        Object genresRaw = doc.get("genres");
        if (genresRaw instanceof List<?> genreList) {
            for (Object item : genreList) {
                if (item instanceof String genreId) {
                    genreRepository.findById(genreId).ifPresent(g ->
                            genres.add(new GenreInfo(g.getId(), g.getName()))
                    );
                }
            }
        }
        dto.setGenres(genres);
        
        Object active = doc.get("isActive");
        if (active instanceof Boolean) {
            dto.setActive((Boolean) active);
        }
        
        return dto;
    }

    /**
     * Enriches starcast data with full artist and artist type information.
     * 
     * AVOIDS N+1 QUERIES:
     * - Extracts all unique artistIds and artistTypeIds in one pass
     * - Batch queries artists collection with IN operator
     * - Batch queries artist_types collection with IN operator
     * - Maps results for O(1) lookup
     * - Returns enriched starcast list
     * 
     * @param starcastRaw Raw starcast array from movie document
     * @return List of enriched StarcastInfo objects
     */
    private List<StarcastInfo> enrichStarcast(Object starcastRaw) {
        List<StarcastInfo> enrichedList = new ArrayList<>();
        
        if (!(starcastRaw instanceof List<?>)) {
            return enrichedList;
        }

        List<?> starcastList = (List<?>) starcastRaw;
        if (starcastList.isEmpty()) {
            return enrichedList;
        }

        // Step 1: Collect all unique artistIds and artistTypeIds
        Set<String> artistIds = new HashSet<>();
        Set<String> artistTypeIds = new HashSet<>();
        List<Map<String, Object>> starcastMaps = new ArrayList<>();

        for (Object item : starcastList) {
            if (item instanceof Map starcastItem) {
                starcastMaps.add(starcastItem);
                String artistId = (String) starcastItem.get("artistId");
                String artistTypeId = (String) starcastItem.get("artistTypeId");
                if (artistId != null) artistIds.add(artistId);
                if (artistTypeId != null) artistTypeIds.add(artistTypeId);
            }
        }

        // Step 2: Batch query artists collection
        Map<String, ArtistInfo> artistMap = new HashMap<>();
        if (!artistIds.isEmpty()) {
            List<ObjectId> artistObjectIds = artistIds.stream()
                    .map(id -> {
                        try {
                            return new ObjectId(id);
                        } catch (Exception e) {
                            log.warn("Invalid ObjectId format for artist: {}", id);
                            return null;
                        }
                    })
                    .filter(obj -> obj != null)
                    .collect(Collectors.toList());

            if (!artistObjectIds.isEmpty()) {
                Query artistQuery = new Query(
                        Criteria.where("_id").in(artistObjectIds)
                                .and("isActive").is(true)
                                .and("deletedAt").is(null)
                );
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> artistDocs = (List<Map<String, Object>>) (List<?>) mongoTemplate.find(artistQuery, Map.class, "artists");
                
                for (Map<String, Object> artistDoc : artistDocs) {
                    String id = artistDoc.get("_id") != null ? artistDoc.get("_id").toString() : null;
                    ArtistInfo artistInfo = ArtistInfo.builder()
                            .id(id)
                            .fullName((String) artistDoc.get("full_name"))
                            .avatar((String) artistDoc.get("avatar"))
                            .rating(artistDoc.get("rating") != null ? ((Number) artistDoc.get("rating")).doubleValue() : null)
                            .bio((String) artistDoc.get("bio"))
                            .build();
                    if (id != null) {
                        artistMap.put(id, artistInfo);
                    }
                }
            }
        }

        // Step 3: Batch query artist_types collection
        Map<String, ArtistTypeInfo> artistTypeMap = new HashMap<>();
        if (!artistTypeIds.isEmpty()) {
            Query artistTypeQuery = new Query(
                    Criteria.where("_id").in(artistTypeIds.stream()
                            .map(ObjectId::new)
                            .collect(Collectors.toList()))
                            .and("isActive").is(true)
                            .and("deletedAt").is(null)
            );
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> artistTypeDocs = (List<Map<String, Object>>) (List<?>) mongoTemplate.find(artistTypeQuery, Map.class, "artist_types");
            
            for (Map<String, Object> typeDoc : artistTypeDocs) {
                String id = typeDoc.get("_id") != null ? typeDoc.get("_id").toString() : null;
                ArtistTypeInfo typeInfo = ArtistTypeInfo.builder()
                        .id(id)
                        .name((String) typeDoc.get("name"))
                        .icon((String) typeDoc.get("icon"))
                        .description((String) typeDoc.get("description"))
                        .build();
                if (id != null) {
                    artistTypeMap.put(id, typeInfo);
                }
            }
        }

        // Step 4: Build enriched starcast list
        for (Map<String, Object> starcastItem : starcastMaps) {
            String artistId = (String) starcastItem.get("artistId");
            String artistTypeId = (String) starcastItem.get("artistTypeId");
            String characterName = (String) starcastItem.get("characterName");

            StarcastInfo enrichedItem = StarcastInfo.builder()
                    .characterName(characterName)
                    .artistId(artistId)
                    .artist(artistId != null ? artistMap.get(artistId) : null)
                    .artistTypeId(artistTypeId)
                    .artistType(artistTypeId != null ? artistTypeMap.get(artistTypeId) : null)
                    .build();

            enrichedList.add(enrichedItem);
        }

        return enrichedList;
    }
}
