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
import java.util.List;
import java.util.Map;
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
            List<Map> movies = mongoTemplate.find(query, Map.class, "movies");
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
                    .starcast(doc.get("starcast"))
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
}
