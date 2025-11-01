package com.awal.cineq.media.controller;

import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.media.dto.response.MediaResponse;
import com.awal.cineq.media.dto.response.MediaListResponse;
import com.awal.cineq.media.service.MediaServiceFactory;
import com.awal.cineq.media.dto.request.MediaDeleteRequestDto;
import com.awal.cineq.media.dto.request.FolderCreateRequest;
import com.awal.cineq.media.dto.request.MediaGetRequest;
import com.awal.cineq.media.dto.request.MediaUploadRequestDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.awal.cineq.media.service.MediaService;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/media")
public class MediaController {
    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    @Autowired
    private MediaServiceFactory mediaServiceFactory;

    // Get all files and folders by parent ID - accepts request DTO with optional parent ID
    @GetMapping({"", "/"})
    public ApiResponse<MediaListResponse> getMedia(@RequestParam(value = "parentId", required = false) UUID parentId) {
        logger.info("Start: getMedia, parentId={}", parentId);
        try {
            MediaService mediaService = mediaServiceFactory.getMediaService();
            MediaListResponse result = mediaService.getMediaByParentId(parentId);
            logger.info("End: getMedia, returnedCount={}", result.getTotalCount());
            return ApiResponse.success("Media retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Error in getMedia: {}", e.getMessage());
            throw new BusinessException("Failed to retrieve media: " + e.getMessage());
        }
    }

    // New endpoint: return all active folders as a flat list
    @GetMapping("/folder")
    public ApiResponse<MediaListResponse> getAllFolders() {
        logger.info("Start: getAllFolders");
        try {
            MediaService mediaService = mediaServiceFactory.getMediaService();
            MediaListResponse result = mediaService.getAllFolders();
            logger.info("End: getAllFolders, folderCount={}", result.getTotalCount());
            return ApiResponse.success("Folders retrieved successfully", result);
        } catch (Exception e) {
            logger.error("Error in getAllFolders: {}", e.getMessage());
            throw new BusinessException("Failed to retrieve folders: " + e.getMessage());
        }
    }

    @PostMapping("/upload-multiple")
    public ApiResponse<MediaResponse> uploadMultipleFiles(@ModelAttribute MediaUploadRequestDto requestDto) {
        logger.info("Start: uploadMultipleFiles, parentId={}, filesCount={}", requestDto.getParentId(), requestDto.getFiles() != null ? requestDto.getFiles().size() : 0);
        try {
            MediaService mediaService = mediaServiceFactory.getMediaService();
            MediaResponse result = mediaService.uploadMultipleFiles(requestDto);
            logger.info("End: uploadMultipleFiles, uploadedFilesCount={}", result.getMedia() != null ? result.getMedia().size() : 0);
            return ApiResponse.success("Files uploaded successfully", result);
        } catch (Exception e) {
            logger.error("Error in uploadMultipleFiles: {}", e.getMessage());
            throw new BusinessException("File upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-multiple")
    public ApiResponse<MediaResponse> deleteMultipleFiles(@RequestBody @Valid MediaDeleteRequestDto mediaDeleteRequests) {
        try {
            MediaService mediaService = mediaServiceFactory.getMediaService();
            List<Map<String, Object>> result = mediaService.deleteMultipleFiles(mediaDeleteRequests);
            logger.info("End: deleteMultipleFiles, processed={}", result.size());
            MediaResponse mediaResponse = new MediaResponse(result);
            return ApiResponse.success("Delete operation completed", mediaResponse);
        } catch (Exception e) {
            logger.error("Error in deleteMultipleFiles: {}", e.getMessage());
            throw new BusinessException("Delete operation failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<MediaResponse> deleteSingleFile(@PathVariable("id") UUID mediaId) {
        logger.info("Start: deleteSingleFile (path), id={}", mediaId);
        try {
            MediaService mediaService = mediaServiceFactory.getMediaService();
            MediaDeleteRequestDto dto = new MediaDeleteRequestDto();
            dto.mediaIds = Collections.singletonList(mediaId);
            Map<String, Object> result = mediaService.deleteSingleFile(dto);
            logger.info("End: deleteSingleFile (path), id={}, status={}", mediaId, result.get("status"));
            MediaResponse mediaResponse = new MediaResponse(Collections.singletonList(result));
            return ApiResponse.success("Delete operation completed", mediaResponse);
        } catch (Exception e) {
            logger.error("Error in deleteSingleFile: {}", e.getMessage());
            throw new BusinessException("Delete operation failed: " + e.getMessage());
        }
    }

    // Alternate single-delete endpoint that accepts a MediaDeleteRequestDto in the request body.
    @PostMapping("/delete-single")
    public ApiResponse<MediaResponse> deleteSingleFileBody(@RequestBody MediaDeleteRequestDto mediaDeleteRequestDto) {
        logger.info("Start: deleteSingleFile (body)");
        try {
            MediaService mediaService = mediaServiceFactory.getMediaService();
            Map<String, Object> result = mediaService.deleteSingleFile(mediaDeleteRequestDto);
            logger.info("End: deleteSingleFile (body), id={}, status={}", result.get("id"), result.get("status"));
            MediaResponse mediaResponse = new MediaResponse(Collections.singletonList(result));
            return ApiResponse.success("Delete operation completed", mediaResponse);
        } catch (Exception e) {
            logger.error("Error in deleteSingleFile (body): {}", e.getMessage());
            throw new BusinessException("Delete operation failed: " + e.getMessage());
        }
    }

    // Create a folder endpoint - accepts folder name and optional parent ID
    @PostMapping("/create-folder")
    public ApiResponse<MediaResponse> createFolder(@RequestBody @Valid FolderCreateRequest request) {
        logger.info("Start: createFolder, folderName={}, parentId={}", request.getName(), request.getParentId());
        try {
            MediaService mediaService = mediaServiceFactory.getMediaService();
            MediaResponse result = mediaService.createFolder(request);
            logger.info("End: createFolder, folderCount={}", result.getMedia() != null ? result.getMedia().size() : 0);
            return ApiResponse.success("Folder created successfully", result);
        } catch (Exception e) {
            logger.error("Error in createFolder: {}", e.getMessage());
            throw new BusinessException("Folder creation failed: " + e.getMessage());
        }
    }

}