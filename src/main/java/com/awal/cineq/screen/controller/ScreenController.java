package com.awal.cineq.screen.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.screen.dto.ScreenDTO;
import com.awal.cineq.screen.service.ScreenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Screen endpoints
 * Provides CRUD operations for cinema screens
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("screen")
@Slf4j
public class ScreenController {

    private final ScreenService screenService;

    /**
     * Get all screens for a specific theatre
     * @param theatreId Theatre identifier
     * @return List of screens for the theatre
     */
    @GetMapping("/theatre/{theatreId}")
    public ResponseEntity<ApiResponse<List<ScreenDTO>>> getScreensByTheatre(
            @PathVariable String theatreId,
            HttpServletRequest request) {
        log.info("getScreensByTheatre STARTED: theatreId={}", theatreId);
        try {
            List<ScreenDTO> screens = screenService.getScreensByTheatreId(theatreId);
            ApiResponse<List<ScreenDTO>> response = ApiResponse.success(
                    "Screens retrieved successfully for theatre: " + theatreId,
                    screens
            );
            response.setPath(request.getRequestURI());
            log.info("getScreensByTheatre END: found {} screens", screens.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getScreensByTheatre ERROR", e);
            throw e;
        }
    }

    /**
     * Get paginated screens for a specific theatre
     * @param theatreId Theatre identifier
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Paginated response with screens
     */
    @GetMapping("/theatre/{theatreId}/paginated")
    public ResponseEntity<PaginationResponse<ScreenDTO>> getScreensByTheatrePaginated(
            @PathVariable String theatreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("getScreensByTheatrePaginated STARTED: theatreId={}, page={}, size={}", theatreId, page, size);
        try {
            PaginationResponse<ScreenDTO> response = screenService.getScreensByTheatreIdPaginated(theatreId, page, size);
            log.info("getScreensByTheatrePaginated END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getScreensByTheatrePaginated ERROR", e);
            throw e;
        }
    }

    /**
     * Get screen by ID
     * @param id Screen identifier
     * @return Screen details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScreenDTO>> getScreenById(
            @PathVariable String id,
            HttpServletRequest request) {
        log.info("getScreenById STARTED: id={}", id);
        try {
            ScreenDTO screen = screenService.getScreenById(id);
            ApiResponse<ScreenDTO> response = ApiResponse.success(
                    "Screen retrieved successfully",
                    screen
            );
            response.setPath(request.getRequestURI());
            log.info("getScreenById END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getScreenById ERROR", e);
            throw e;
        }
    }

    /**
     * Get all screens
     * @return List of all screens
     */
    @GetMapping(path = {"", "/"})
    public ResponseEntity<ApiResponse<List<ScreenDTO>>> getAllScreens(HttpServletRequest request) {
        log.info("getAllScreens STARTED");
        try {
            List<ScreenDTO> screens = screenService.getAllScreens();
            ApiResponse<List<ScreenDTO>> response = ApiResponse.success(
                    "All screens retrieved successfully",
                    screens
            );
            response.setPath(request.getRequestURI());
            log.info("getAllScreens END: found {} screens", screens.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getAllScreens ERROR", e);
            throw e;
        }
    }

    /**
     * Get all screens (paginated)
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Paginated response with screens
     */
    @GetMapping("/paginated")
    public ResponseEntity<PaginationResponse<ScreenDTO>> getAllScreensPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("getAllScreensPaginated STARTED: page={}, size={}", page, size);
        try {
            PaginationResponse<ScreenDTO> response = screenService.getAllScreensPaginated(page, size);
            log.info("getAllScreensPaginated END");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getAllScreensPaginated ERROR", e);
            throw e;
        }
    }

    /**
     * Get count of screens in a theatre
     * @param theatreId Theatre identifier
     * @return Count response
     */
    @GetMapping("/theatre/{theatreId}/count")
    public ResponseEntity<ApiResponse<Long>> getScreenCountByTheatre(
            @PathVariable String theatreId,
            HttpServletRequest request) {
        log.info("getScreenCountByTheatre STARTED: theatreId={}", theatreId);
        try {
            long count = screenService.getScreenCountByTheatre(theatreId);
            ApiResponse<Long> response = ApiResponse.success(
                    "Screen count retrieved successfully",
                    count
            );
            response.setPath(request.getRequestURI());
            log.info("getScreenCountByTheatre END: count={}", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getScreenCountByTheatre ERROR", e);
            throw e;
        }
    }
}
