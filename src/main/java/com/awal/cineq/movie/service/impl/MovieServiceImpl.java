package com.awal.cineq.movie.service.impl;

import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.movie.dto.MovieDTO;
import com.awal.cineq.movie.dto.request.MovieRequest;
import com.awal.cineq.movie.model.Movie;
import com.awal.cineq.movie.repository.MovieRepository;
import com.awal.cineq.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Movie operations
 * Uses MongoDB via Spring Data MongoDB
 * All IDs are MongoDB ObjectIds stored as String
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {
    
    private final MovieRepository movieRepository;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<MovieDTO> getAllMovies(int page, int size, String sortBy, String sortDirection) {
        log.info("getAllMovies STARTED: page={}, size={}, sortBy={}, sortDirection={}", 
                 page, size, sortBy, sortDirection);
        
        try {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortDirection) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
            PageRequest pageReq = PageRequest.of(page - 1, size, Sort.by(dir, sortBy));
            
            Page<Movie> movies = movieRepository.findAll(pageReq);
            
            List<MovieDTO> dtos = movies.stream()
                .map(movie -> modelMapper.map(movie, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("getAllMovies END: found {} movies", dtos.size());
            
            return PaginationResponse.success("Fetched successfully", dtos,
                movies.getNumber() + 1,
                movies.getSize(),
                movies.getTotalPages(),
                movies.getTotalElements(),
                movies.hasNext(),
                movies.hasPrevious());
                
        } catch (Exception e) {
            log.error("getAllMovies ERROR", e);
            throw new BusinessException("Failed to fetch movies", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public MovieDTO getMovieById(String id) {
        log.info("getMovieById STARTED: id={}", id);
        
        try {
            Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
            
            log.info("getMovieById END: found movie {}", movie.getTitle());
            return modelMapper.map(movie, MovieDTO.class);
            
        } catch (ResourceNotFoundException e) {
            log.warn("getMovieById: Movie not found for id={}", id);
            throw e;
        } catch (Exception e) {
            log.error("getMovieById ERROR", e);
            throw new BusinessException("Failed to fetch movie", e);
        }
    }
    
    @Override
    public MovieDTO createMovie(MovieRequest request) {
        log.info("createMovie STARTED: title={}", request.getTitle());
        
        try {
            Movie movie = new Movie();
            movie.setTitle(request.getTitle());
            movie.setDescription(request.getDescription());
            movie.setDurationMinutes(request.getDurationMinutes());
            movie.setReleaseDate(request.getReleaseDate());
            movie.setRating(request.getRating());
            movie.setLanguage(request.getLanguage());
            movie.setPosterUrl(request.getPosterUrl());
            movie.setTrailerUrl(request.getTrailerUrl());
            movie.setDirector(request.getDirector());
            movie.setCastMembers(request.getCastMembers());
            movie.setPrice(request.getPrice());
            movie.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            movie.setGenreIds(request.getGenreIds());
            
            Movie saved = movieRepository.save(movie);
            log.info("createMovie END: id={}", saved.getId());
            
            return modelMapper.map(saved, MovieDTO.class);
            
        } catch (Exception e) {
            log.error("createMovie ERROR", e);
            throw new BusinessException("Failed to create movie", e);
        }
    }
    
    @Override
    public MovieDTO updateMovie(String id, MovieRequest request) {
        log.info("updateMovie STARTED: id={}, title={}", id, request.getTitle());
        
        try {
            Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
            
            // Check soft-delete
            if (movie.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Movie not found with id: " + id);
            }
            
            if (request.getTitle() != null) {
                movie.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                movie.setDescription(request.getDescription());
            }
            if (request.getDurationMinutes() != null) {
                movie.setDurationMinutes(request.getDurationMinutes());
            }
            if (request.getReleaseDate() != null) {
                movie.setReleaseDate(request.getReleaseDate());
            }
            if (request.getRating() != null) {
                movie.setRating(request.getRating());
            }
            if (request.getLanguage() != null) {
                movie.setLanguage(request.getLanguage());
            }
            if (request.getPosterUrl() != null) {
                movie.setPosterUrl(request.getPosterUrl());
            }
            if (request.getTrailerUrl() != null) {
                movie.setTrailerUrl(request.getTrailerUrl());
            }
            if (request.getDirector() != null) {
                movie.setDirector(request.getDirector());
            }
            if (request.getCastMembers() != null) {
                movie.setCastMembers(request.getCastMembers());
            }
            if (request.getPrice() != null) {
                movie.setPrice(request.getPrice());
            }
            if (request.getIsActive() != null) {
                movie.setIsActive(request.getIsActive());
            }
            if (request.getGenreIds() != null) {
                movie.setGenreIds(request.getGenreIds());
            }
            
            Movie updated = movieRepository.save(movie);
            log.info("updateMovie END: id={}", updated.getId());
            
            return modelMapper.map(updated, MovieDTO.class);
            
        } catch (ResourceNotFoundException e) {
            log.warn("updateMovie: Movie not found for id={}", id);
            throw e;
        } catch (Exception e) {
            log.error("updateMovie ERROR", e);
            throw new BusinessException("Failed to update movie", e);
        }
    }
    
    @Override
    public void deleteMovie(String id) {
        log.info("deleteMovie STARTED: id={}", id);
        
        try {
            Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
            
            if (movie.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Movie not found with id: " + id);
            }
            
            // Soft delete: set deletedAt timestamp
            movie.setDeletedAt(LocalDateTime.now());
            movieRepository.save(movie);
            
            log.info("deleteMovie END: soft-deleted id={}", id);
            
        } catch (ResourceNotFoundException e) {
            log.warn("deleteMovie: Movie not found for id={}", id);
            throw e;
        } catch (Exception e) {
            log.error("deleteMovie ERROR", e);
            throw new BusinessException("Failed to delete movie", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> searchByTitle(String title) {
        log.info("searchByTitle STARTED: title={}", title);
        
        try {
            List<Movie> movies = movieRepository.findByTitleContainingIgnoreCase(title);
            
            List<MovieDTO> dtos = movies.stream()
                .filter(m -> m.getDeletedAt() == null)  // Exclude soft-deleted
                .map(m -> modelMapper.map(m, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("searchByTitle END: found {} movies", dtos.size());
            return dtos;
            
        } catch (Exception e) {
            log.error("searchByTitle ERROR", e);
            throw new BusinessException("Failed to search movies", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> getActiveMovies() {
        log.info("getActiveMovies STARTED");
        
        try {
            List<Movie> movies = movieRepository.findByIsActiveTrue();
            
            List<MovieDTO> dtos = movies.stream()
                .map(m -> modelMapper.map(m, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("getActiveMovies END: found {} movies", dtos.size());
            return dtos;
            
        } catch (Exception e) {
            log.error("getActiveMovies ERROR", e);
            throw new BusinessException("Failed to fetch active movies", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> getCurrentlyShowingMovies() {
        log.info("getCurrentlyShowingMovies STARTED");
        
        try {
            List<Movie> movies = movieRepository.findCurrentlyShowingMovies(LocalDate.now());
            
            List<MovieDTO> dtos = movies.stream()
                .map(m -> modelMapper.map(m, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("getCurrentlyShowingMovies END: found {} movies", dtos.size());
            return dtos;
            
        } catch (Exception e) {
            log.error("getCurrentlyShowingMovies ERROR", e);
            throw new BusinessException("Failed to fetch currently showing movies", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> getUpcomingMovies() {
        log.info("getUpcomingMovies STARTED");
        
        try {
            List<Movie> movies = movieRepository.findUpcomingMovies(LocalDate.now());
            
            List<MovieDTO> dtos = movies.stream()
                .map(m -> modelMapper.map(m, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("getUpcomingMovies END: found {} movies", dtos.size());
            return dtos;
            
        } catch (Exception e) {
            log.error("getUpcomingMovies ERROR", e);
            throw new BusinessException("Failed to fetch upcoming movies", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> findByLanguage(String language) {
        log.info("findByLanguage STARTED: language={}", language);
        
        try {
            List<Movie> movies = movieRepository.findByLanguage(language);
            
            List<MovieDTO> dtos = movies.stream()
                .filter(m -> m.getDeletedAt() == null)  // Exclude soft-deleted
                .map(m -> modelMapper.map(m, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("findByLanguage END: found {} movies", dtos.size());
            return dtos;
            
        } catch (Exception e) {
            log.error("findByLanguage ERROR", e);
            throw new BusinessException("Failed to find movies by language", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> findByRating(String rating) {
        log.info("findByRating STARTED: rating={}", rating);
        
        try {
            List<Movie> movies = movieRepository.findByRating(rating);
            
            List<MovieDTO> dtos = movies.stream()
                .filter(m -> m.getDeletedAt() == null)  // Exclude soft-deleted
                .map(m -> modelMapper.map(m, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("findByRating END: found {} movies", dtos.size());
            return dtos;
            
        } catch (Exception e) {
            log.error("findByRating ERROR", e);
            throw new BusinessException("Failed to find movies by rating", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MovieDTO> findByGenreId(String genreId) {
        log.info("findByGenreId STARTED: genreId={}", genreId);
        
        try {
            List<Movie> movies = movieRepository.findByGenreId(genreId);
            
            List<MovieDTO> dtos = movies.stream()
                .map(m -> modelMapper.map(m, MovieDTO.class))
                .collect(Collectors.toList());
            
            log.info("findByGenreId END: found {} movies", dtos.size());
            return dtos;
            
        } catch (Exception e) {
            log.error("findByGenreId ERROR", e);
            throw new BusinessException("Failed to find movies by genre", e);
        }
    }
    
    @Override
    public MovieDTO toggleMovieStatus(String id, boolean isActive) {
        log.info("toggleMovieStatus STARTED: id={}, isActive={}", id, isActive);
        
        try {
            Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
            
            if (movie.getDeletedAt() != null) {
                throw new ResourceNotFoundException("Movie not found with id: " + id);
            }
            
            movie.setIsActive(isActive);
            Movie updated = movieRepository.save(movie);
            
            log.info("toggleMovieStatus END: id={}", updated.getId());
            return modelMapper.map(updated, MovieDTO.class);
            
        } catch (ResourceNotFoundException e) {
            log.warn("toggleMovieStatus: Movie not found for id={}", id);
            throw e;
        } catch (Exception e) {
            log.error("toggleMovieStatus ERROR", e);
            throw new BusinessException("Failed to toggle movie status", e);
        }
    }
    
    @Override
    public void bulkUpdateMovieStatus(List<String> ids, boolean isActive) {
        log.info("bulkUpdateMovieStatus STARTED: count={}, isActive={}", ids.size(), isActive);
        
        try {
            for (String id : ids) {
                try {
                    toggleMovieStatus(id, isActive);
                } catch (ResourceNotFoundException e) {
                    log.warn("bulkUpdateMovieStatus: Skipping non-existent id={}", id);
                }
            }
            
            log.info("bulkUpdateMovieStatus END: updated {} movies", ids.size());
            
        } catch (Exception e) {
            log.error("bulkUpdateMovieStatus ERROR", e);
            throw new BusinessException("Failed to bulk update movie status", e);
        }
    }
}
