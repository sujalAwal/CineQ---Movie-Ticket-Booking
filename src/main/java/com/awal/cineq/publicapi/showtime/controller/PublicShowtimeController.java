package com.awal.cineq.publicapi.showtime.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.publicapi.showtime.dto.SeatAvailabilityResponse;
import com.awal.cineq.publicapi.showtime.dto.ShowtimeListDTO;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicRequest;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicResponse;
import com.awal.cineq.publicapi.showtime.dto.SuggestSeatsRequest;
import com.awal.cineq.publicapi.showtime.dto.SuggestSeatsResponse;
import com.awal.cineq.publicapi.showtime.service.PublicShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/showtimes")
@RequiredArgsConstructor
@Slf4j
public class PublicShowtimeController {

    private final PublicShowtimeService publicShowtimeService;

    @GetMapping
    public PaginationResponse<ShowtimeListDTO> getAllShowtimes(ShowtimePageRequest pageRequest) {
        log.info("STARTED GET /public/showtimes");
        PaginationResponse<ShowtimeListDTO> result = publicShowtimeService.getAllShowtimes(pageRequest);
        if (result == null) {
            log.error("ERROR GET /public/showtimes: null result");
            throw new ResourceNotFoundException("No showtimes found");
        }
        log.info("END GET /public/showtimes");
        return result;
    }

    @GetMapping("/movie/{movieId}")
    public PaginationResponse<ShowtimeListDTO> getShowtimesByMovieId(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("STARTED GET /public/showtimes/movie/{}", movieId);
        PaginationResponse<ShowtimeListDTO> result = publicShowtimeService.getShowtimesByMovieId(movieId, page, size);
        if (result == null) {
            log.error("ERROR GET /public/showtimes/movie/{}: null result", movieId);
            throw new ResourceNotFoundException("No showtimes found for movie: " + movieId);
        }
        log.info("END GET /public/showtimes/movie/{}", movieId);
        return result;
    }

    @GetMapping("/theatre/{theatreId}")
    public PaginationResponse<ShowtimeListDTO> getShowtimesByTheatreId(
            @PathVariable String theatreId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("STARTED GET /public/showtimes/theatre/{}", theatreId);
        PaginationResponse<ShowtimeListDTO> result = publicShowtimeService.getShowtimesByTheatreId(theatreId, page, size);
        if (result == null) {
            log.error("ERROR GET /public/showtimes/theatre/{}: null result", theatreId);
            throw new ResourceNotFoundException("No showtimes found for theatre: " + theatreId);
        }
        log.info("END GET /public/showtimes/theatre/{}", theatreId);
        return result;
    }

    // CRITICAL: /suggest-seats and /{id}/seats MUST be declared BEFORE /{id} to prevent Spring
    // from matching "suggest-seats" or "seats" as path variable values for the {id} mapping.
    @PostMapping("/suggest-seats")
    public ResponseEntity<ApiResponse<SuggestSeatsResponse>> suggestSeats(
            @Valid @RequestBody SuggestSeatsRequest request) {
        log.info("STARTED POST /public/showtimes/suggest-seats with showtimeId={}, seats={}", 
                request.getShowtimeId(), request.getSeats());
        ApiResponse<SuggestSeatsResponse> response = publicShowtimeService.suggestSeats(request);
        log.info("END POST /public/showtimes/suggest-seats");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<SeatAvailabilityResponse>> getSeatAvailability(
            @PathVariable String id) {
        log.info("STARTED GET /public/showtimes/{}/seats", id);
        ApiResponse<SeatAvailabilityResponse> response = publicShowtimeService.getSeatAvailability(id);
        log.info("END GET /public/showtimes/{}/seats", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getShowtimeById(@PathVariable String id) {
        log.info("STARTED GET /public/showtimes/{}", id);
        ApiResponse<Object> response = publicShowtimeService.getShowtimeById(id);
        log.info("END GET /public/showtimes/{}", id);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // POST /public/showtimes/{id}/bookings — get public booking details
    // ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/bookings")
    public ResponseEntity<ApiResponse<List<BookingPublicResponse>>> getPublicBookingsByShowtime(
            @PathVariable String id,
            @RequestBody BookingPublicRequest request) {
        
        // Override showtimeId from request with path variable for security
        request.setShowtimeId(id);
        log.info("STARTED POST /public/showtimes/{}/bookings", id);
        List<BookingPublicResponse> bookings = publicShowtimeService.getPublicBookingsByShowtime(request);
        log.info("END POST /public/showtimes/{}/bookings", id);
        return ResponseEntity.ok(ApiResponse.success("Public bookings retrieved successfully", bookings));
    }
}
