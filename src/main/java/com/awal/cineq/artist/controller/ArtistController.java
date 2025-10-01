package com.awal.cineq.artist.controller;

import com.awal.cineq.artist.dto.ArtistDTO;
import com.awal.cineq.artist.dto.ArtistRequestDto;
import com.awal.cineq.artist.dto.BulkArtistStatusUpdateRequest;
import com.awal.cineq.artist.service.ArtistService;
import com.awal.cineq.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/artist")
@RequiredArgsConstructor
public class ArtistController {
    private final ArtistService artistService;

    @PostMapping
    public ResponseEntity<ApiResponse<ArtistDTO>> createArtist(@RequestBody ArtistRequestDto requestDto) {
        ArtistDTO artist = artistService.createArtist(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Artist created successfully", artist));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtistDTO>> updateArtist(@PathVariable UUID id, @RequestBody ArtistRequestDto requestDto) {
        ArtistDTO artist = artistService.updateArtist(id, requestDto);
        return ResponseEntity.ok(ApiResponse.success("Artist updated successfully", artist));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArtist(@PathVariable UUID id) {
        artistService.deleteArtist(id);
        return ResponseEntity.ok(ApiResponse.success("Artist deleted successfully", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtistDTO>> getArtist(@PathVariable UUID id) {
        ArtistDTO artist = artistService.getArtist(id);
        return ResponseEntity.ok(ApiResponse.success("Artist fetched successfully", artist));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ArtistDTO>>> getAllArtists() {
        List<ArtistDTO> artists = artistService.getAllArtists();
        return ResponseEntity.ok(ApiResponse.success("Artists fetched successfully", artists));
    }

    @GetMapping("/active/{isActive}")
    public ResponseEntity<ApiResponse<List<ArtistDTO>>> getArtistsByActiveStatus(@PathVariable Boolean isActive) {
        List<ArtistDTO> artists = artistService.getArtistsByActiveStatus(isActive);
        return ResponseEntity.ok(ApiResponse.success("Artists fetched successfully", artists));
    }

    @PostMapping("/bulk-enable")
    public ResponseEntity<ApiResponse<Void>> bulkEnable(@RequestBody BulkArtistStatusUpdateRequest request) {
        artistService.bulkEnable(request);
        return ResponseEntity.ok(ApiResponse.success("Artists enabled successfully", null));
    }

    @PostMapping("/bulk-disable")
    public ResponseEntity<ApiResponse<Void>> bulkDisable(@RequestBody BulkArtistStatusUpdateRequest request) {
        artistService.bulkDisable(request);
        return ResponseEntity.ok(ApiResponse.success("Artists disabled successfully", null));
    }
}
