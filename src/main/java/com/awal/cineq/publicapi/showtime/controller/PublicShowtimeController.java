package com.awal.cineq.publicapi.showtime.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.publicapi.showtime.dto.SeatAvailabilityResponse;
import com.awal.cineq.publicapi.showtime.service.PublicShowtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/showtimes")
@RequiredArgsConstructor
@Slf4j
public class PublicShowtimeController {

    private final PublicShowtimeService publicShowtimeService;

    @GetMapping
    public PaginationResponse<ShowtimeDTO> getAllShowtimes(ShowtimePageRequest pageRequest) {
        log.info("STARTED GET /public/showtimes");
        PaginationResponse<ShowtimeDTO> result = publicShowtimeService.getAllShowtimes(pageRequest);
        if (result == null) {
            log.error("ERROR GET /public/showtimes: null result");
            throw new ResourceNotFoundException("No showtimes found");
        }
        log.info("END GET /public/showtimes");
        return result;
    }

    @GetMapping("/movie/{movieId}")
    public PaginationResponse<ShowtimeDTO> getShowtimesByMovieId(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("STARTED GET /public/showtimes/movie/{}", movieId);
        PaginationResponse<ShowtimeDTO> result = publicShowtimeService.getShowtimesByMovieId(movieId, page, size);
        if (result == null) {
            log.error("ERROR GET /public/showtimes/movie/{}: null result", movieId);
            throw new ResourceNotFoundException("No showtimes found for movie: " + movieId);
        }
        log.info("END GET /public/showtimes/movie/{}", movieId);
        return result;
    }

    @GetMapping("/theatre/{theatreId}")
    public PaginationResponse<ShowtimeDTO> getShowtimesByTheatreId(
            @PathVariable String theatreId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("STARTED GET /public/showtimes/theatre/{}", theatreId);
        PaginationResponse<ShowtimeDTO> result = publicShowtimeService.getShowtimesByTheatreId(theatreId, page, size);
        if (result == null) {
            log.error("ERROR GET /public/showtimes/theatre/{}: null result", theatreId);
            throw new ResourceNotFoundException("No showtimes found for theatre: " + theatreId);
        }
        log.info("END GET /public/showtimes/theatre/{}", theatreId);
        return result;
    }

    // CRITICAL: /{id}/seats MUST be declared BEFORE /{id} to prevent Spring
    // from matching "seats" as a path variable value for the {id} mapping.
    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<SeatAvailabilityResponse>> getSeatAvailability(
            @PathVariable String id) {
        log.info("STARTED GET /public/showtimes/{}/seats", id);
        ApiResponse<SeatAvailabilityResponse> response = publicShowtimeService.getSeatAvailability(id);
        log.info("END GET /public/showtimes/{}/seats", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeDTO>> getShowtimeById(@PathVariable String id) {
        log.info("STARTED GET /public/showtimes/{}", id);
        ApiResponse<ShowtimeDTO> response = publicShowtimeService.getShowtimeById(id);
        log.info("END GET /public/showtimes/{}", id);
        return ResponseEntity.ok(response);
    }
}
