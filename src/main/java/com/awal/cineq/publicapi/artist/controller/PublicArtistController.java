package com.awal.cineq.publicapi.artist.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.publicapi.artist.dto.PublicArtistResponse;
import com.awal.cineq.publicapi.artist.service.PublicArtistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/artists")
@RequiredArgsConstructor
@Slf4j
public class PublicArtistController {

    private final PublicArtistService publicArtistService;

    /**
     * Get all active artists
     * Optional query param: artistTypeId to filter by artist type
     * 
     * GET /api/public/artists
     * GET /api/public/artists?artistTypeId=69d271a73199af61db36b9e6
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicArtistResponse>>> getAllArtists(
            @RequestParam(required = false) String artistTypeId) {
        log.info("getAllArtists REQUEST: artistTypeId={}", artistTypeId);
        ApiResponse<List<PublicArtistResponse>> response = publicArtistService.getAllActiveArtists(artistTypeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get multiple artists by IDs
     * Required query param: ids (comma-separated)
     * Optional query param: artistTypeId to filter by artist type
     * 
     * GET /api/public/artists/by-ids?ids=69d7ef09bfe5b7a48d63fe61,69d7f234bfe5b7a48d63fe65
     * GET /api/public/artists/by-ids?ids=69d7ef09bfe5b7a48d63fe61,69d7f234bfe5b7a48d63fe65&artistTypeId=69d271a73199af61db36b9e6
     */
    @GetMapping("/by-ids")
    public ResponseEntity<ApiResponse<List<PublicArtistResponse>>> getArtistsByIds(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String artistTypeId) {
        log.info("getArtistsByIds REQUEST: ids={}, artistTypeId={}", ids, artistTypeId);
        
        List<String> idList = new java.util.ArrayList<>();
        if (ids != null && !ids.trim().isEmpty()) {
            idList = java.util.Arrays.asList(ids.split(","));
        }
        
        ApiResponse<List<PublicArtistResponse>> response = publicArtistService.getArtistsByIds(idList, artistTypeId);
        return ResponseEntity.ok(response);
    }
}
