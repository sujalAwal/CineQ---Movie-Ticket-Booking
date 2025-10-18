package com.awal.cineq.media.service.impl;

import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.media.dto.response.*;
import com.awal.cineq.media.model.MediaType;
import com.awal.cineq.media.dto.request.MediaDeleteRequestDto;
import com.awal.cineq.media.dto.request.FolderCreateRequest;
import com.awal.cineq.media.dto.request.MediaUploadRequestDto;
import com.awal.cineq.media.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.awal.cineq.media.model.Media;
import com.awal.cineq.media.repository.MediaRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service("localMediaService")
public class MediaServiceImpl implements MediaService {
    private static final Logger logger = LoggerFactory.getLogger(MediaServiceImpl.class);

    @Autowired
    private MediaRepository mediaRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public MediaResponse uploadMultipleFiles(List<MultipartFile> files, UUID parentId) {
        logger.info("Start: uploadMultipleFiles, parentId={}, filesCount={}", parentId, files.size());
        List<Map<String, Object>> uploadedFiles = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            try {
                // Generate title for each file
                String fileTitle = file.getOriginalFilename();

                // Upload single file
                Media media = uploadSingleFile(file, fileTitle, parentId);

                // Create professional DTO for file info
                MediaItemDto fileInfo = MediaItemDto.builder()
                        .id(media.getId())
                        .title(media.getFileName())
                        .url(media.getUrl())
                        .type(media.getType())
                        .originalFileName(file.getOriginalFilename())
                        .build();

                uploadedFiles.add(fileInfo.toMap());
                logger.info("File uploaded successfully: id={}, title={}, type={}", fileInfo.getId(), fileInfo.getTitle(), fileInfo.getType());

            } catch (Exception e) {
                logger.error("Error uploading file: {} - {}", file.getOriginalFilename(), e.getMessage());
                String fname = file != null ? file.getOriginalFilename() : "<unknown>";
                throw new BusinessException("Failed to upload file: " + fname, e);

            }
        }

        logger.info("End: uploadMultipleFiles, uploadedFilesCount={}", uploadedFiles.size());
        return new MediaResponse(uploadedFiles);
    }

    @Override
    public MediaResponse uploadMultipleFiles(MediaUploadRequestDto requestDto) {
        List<MultipartFile> files = requestDto.getFiles();
        UUID parentId = requestDto.getParentId();
        logger.info("Start: uploadMultipleFiles, parentId={}, filesCount={}", parentId, files != null ? files.size() : 0);
        List<Map<String, Object>> uploadedFiles = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            throw new BusinessException("No files provided for upload");
        }
        for (MultipartFile file : files) {
            try {
                String fileTitle = file.getOriginalFilename();
                Media media = uploadSingleFile(file, fileTitle, parentId);
                MediaItemDto fileInfo = MediaItemDto.builder()
                        .id(media.getId())
                        .title(media.getFileName())
                        .url(media.getUrl())
                        .type(media.getType())
                        .originalFileName(file.getOriginalFilename())
                        .build();
                uploadedFiles.add(fileInfo.toMap());
                logger.info("File uploaded successfully: id={}, title={}, type={}", fileInfo.getId(), fileInfo.getTitle(), fileInfo.getType());
            } catch (Exception e) {
                logger.error("Error uploading file: {} - {}", file.getOriginalFilename(), e.getMessage());
                String fname = file != null ? file.getOriginalFilename() : "<unknown>";
                throw new BusinessException("Failed to upload file: " + fname, e);
            }
        }
        logger.info("End: uploadMultipleFiles, uploadedFilesCount={}", uploadedFiles.size());
        return new MediaResponse(uploadedFiles);
    }

    private Media uploadSingleFile(MultipartFile file, String title, UUID parentId) throws IOException {
        logger.info("Start: uploadSingleFile, title={}, parentId={}, originalFileName={}", title, parentId, file.getOriginalFilename());

        // Validate file
        if (file.isEmpty()) {
            logger.error("File is empty: {}", file.getOriginalFilename());
            throw new ResourceNotFoundException("File is empty: " + file.getOriginalFilename());
        }

        // Create upload directory
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String fileName = UUID.randomUUID() + "." + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Determine media type based on file content
        MediaType mediaType = determineMediaType(file.getContentType(), fileExtension);

        // Create and save media entity
        Media media = new Media();
        // Do NOT set media.setId(...)
        media.setFileName(title);
        media.setUrl("/uploads/" + fileName);
        media.setType(mediaType);
        media.setActive(true);
        media.setParentId(parentId);
        media.setCreatedAt(LocalDateTime.now());

        // Save entity first, so Hibernate generates the ID
        Media savedMedia = mediaRepository.save(media);

        // Now you can use savedMedia.getId() for further operations
        // If you need to store file based on type, do it here:
        // String storageUrl = storeFileBasedOnType(file, savedMedia.getId());
        logger.info("End: uploadSingleFile, savedMediaId={}, url={}", savedMedia.getId(), savedMedia.getUrl());
        return savedMedia;
    }

    private String getFileExtension(String fileName) {
        logger.info("getFileExtension called for fileName={}", fileName);
        if (fileName == null || !fileName.contains(".")) {
            logger.warn("File extension not found for fileName={}, defaulting to 'bin'", fileName);
            return "bin";
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        logger.info("File extension determined: {}", ext);
        return ext;
    }

    private MediaType determineMediaType(String contentType, String fileExtension) {
        logger.info("determineMediaType called for contentType={}, fileExtension={}", contentType, fileExtension);
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                logger.info("MediaType set to IMAGE");
                return MediaType.IMAGE;
            } else if (contentType.startsWith("video/")) {
                logger.info("MediaType set to VIDEO");
                return MediaType.VIDEO;
            } else if (contentType.startsWith("audio/")) {
                logger.info("MediaType set to AUDIO");
                return MediaType.AUDIO;
            }
        }

        // Fallback to file extension
        switch (fileExtension.toLowerCase()) {
            case "jpg": case "jpeg": case "png": case "gif": case "bmp":
                logger.info("MediaType set to IMAGE by extension");
                return MediaType.IMAGE;
            case "mp4": case "avi": case "mov": case "wmv":
                logger.info("MediaType set to VIDEO by extension");
                return MediaType.VIDEO;
            case "mp3": case "wav": case "aac":
                logger.info("MediaType set to AUDIO by extension");
                return MediaType.AUDIO;
            default:
                logger.info("MediaType set to DOCUMENT by extension");
                return MediaType.DOCUMENT;
        }
    }

    // Delete multiple media entries using the DTO wrapper. Each DTO may contain multiple UUIDs.
    public List<Map<String, Object>> deleteMultipleFiles(MediaDeleteRequestDto mediaDeleteRequestDto) {
        logger.info("Start: local deleteMultipleFiles, requestsCount={}", mediaDeleteRequestDto == null || mediaDeleteRequestDto.mediaIds == null ? 0 : mediaDeleteRequestDto.mediaIds.size());
        if (mediaDeleteRequestDto == null || mediaDeleteRequestDto.mediaIds == null || mediaDeleteRequestDto.mediaIds.isEmpty()) {
            throw new BusinessException("No media delete requests provided for deletion");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (UUID id : mediaDeleteRequestDto.mediaIds) {
            MediaDeleteRequestDto singleDto = new MediaDeleteRequestDto();
            singleDto.mediaIds = java.util.Collections.singletonList(id);
            results.add(deleteSingleFile(singleDto));
        }

        logger.info("End: local deleteMultipleFiles, processed={}", results.size());
        return results;
    }

    // Delete a single media entry described by MediaDeleteRequestDto (expects first id in list).
    public Map<String, Object> deleteSingleFile(MediaDeleteRequestDto mediaDeleteRequestDto) {
        Map<String, Object> outcome = new HashMap<>();

        if (mediaDeleteRequestDto == null || mediaDeleteRequestDto.mediaIds == null || mediaDeleteRequestDto.mediaIds.isEmpty()) {
            outcome.put("id", null);
            outcome.put("status", "FAILED");
            outcome.put("message", "No mediaId provided in request");
            return outcome;
        }

        UUID mediaId = mediaDeleteRequestDto.mediaIds.get(0);
        outcome.put("id", mediaId);

        if (mediaId == null) {
            outcome.put("status", "FAILED");
            outcome.put("message", "Null id provided");
            return outcome;
        }

        try {
            Media media = mediaRepository.findById(mediaId).orElse(null);
            if (media == null) {
                outcome.put("status", "NOT_FOUND");
                outcome.put("message", "Media not found");
                return outcome;
            }

            // Determine local file path. Prefer filePath if set; otherwise derive filename from URL and resolve under uploadDir.
            String storedPath = media.getFilePath();
            Path targetPath = null;

            if (storedPath != null && !storedPath.trim().isEmpty()) {
                Path p = Paths.get(storedPath);
                if (!p.isAbsolute()) {
                    p = Paths.get(uploadDir).resolve(p).normalize();
                }
                targetPath = p;
            } else if (media.getUrl() != null && !media.getUrl().trim().isEmpty()) {
                String url = media.getUrl();
                String filename = url.substring(url.lastIndexOf('/') + 1);
                targetPath = Paths.get(uploadDir).resolve(filename).normalize();
            }

            if (targetPath != null) {
                try {
                    boolean deleted = Files.deleteIfExists(targetPath);
                    outcome.put("fileDeleted", deleted);
                    if (!deleted) {
                        outcome.put("deleteFromStorage", "FAILED");
                        outcome.put("storageMessage", "File not found on disk: " + targetPath.toString());
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to delete local file for media id {}: {}", mediaId, ex.getMessage());
                    outcome.put("deleteFromStorage", "FAILED");
                    outcome.put("storageMessage", ex.getMessage());
                }
            } else {
                outcome.put("deleteFromStorage", "FAILED");
                outcome.put("storageMessage", "No file path or URL available");
            }

            // Soft delete record
            media.setDeletedAt(LocalDateTime.now());
            mediaRepository.save(media);
            outcome.put("status", "DELETED");

        } catch (Exception e) {
            logger.error("Error deleting media id {}: {}", mediaId, e.getMessage());
            outcome.put("status", "FAILED");
            outcome.put("message", e.getMessage());
        }

        return outcome;
    }

    // Create a folder in the media system without calling any external storage API.
    // Only stores folder metadata in the database.
    @Override
    public MediaResponse createFolder(FolderCreateRequest request) {
        logger.info("Start: createFolder (Local), folderName={}, parentId={}", request.getName(), request.getParentId());

        try {
            // Validate folder name
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                logger.error("Folder name is empty or null");
                throw new BusinessException("Folder name cannot be empty");
            }

            // Validate parent folder exists if parentId is provided
            if (request.getParentId() != null) {
                logger.info("Validating parent folder with id={}", request.getParentId());
                Media parentMedia = mediaRepository.findById(request.getParentId()).orElse(null);
                if (parentMedia == null) {
                    logger.error("Parent folder not found with id={}", request.getParentId());
                    throw new ResourceNotFoundException("Parent folder not found with id: " + request.getParentId());
                }
                if (parentMedia.getType() != MediaType.FOLDER) {
                    logger.error("Parent media is not a folder, type={}", parentMedia.getType());
                    throw new BusinessException("Parent media must be a folder, but found type: " + parentMedia.getType());
                }
                logger.info("Parent folder validated successfully");
            }

            // Create folder entity
            Media folder = new Media();
            folder.setFileName(request.getName().trim());
            folder.setType(MediaType.FOLDER);
            folder.setActive(true);
            folder.setParentId(request.getParentId());
            folder.setFilePath(request.getName().trim()); // Store folder name as path
            folder.setCreatedAt(LocalDateTime.now());
            folder.setUpdatedAt(LocalDateTime.now());

            // Save folder to database
            Media savedFolder = mediaRepository.save(folder);
            logger.info("Folder created successfully with id={}, path={}", savedFolder.getId(), savedFolder.getFilePath());

            // Create professional DTO for folder info
            MediaItemDto folderInfo = MediaItemDto.builder()
                    .id(savedFolder.getId())
                    .name(savedFolder.getFileName())
                    .type(savedFolder.getType())
                    .parentId(savedFolder.getParentId())
                    .filePath(savedFolder.getFilePath())
                    .isActive(savedFolder.isActive())
                    .createdAt(savedFolder.getCreatedAt())
                    .build();

            List<Map<String, Object>> result = new ArrayList<>();
            result.add(folderInfo.toMap());

            logger.info("End: createFolder (Local), folderId={}", savedFolder.getId());
            return new MediaResponse(result);

        } catch (BusinessException | ResourceNotFoundException e) {
            logger.error("Error creating folder: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating folder: {}", e.getMessage(), e);
            throw new BusinessException("Failed to create folder: " + e.getMessage(), e);
        }
    }

    @Override
    public MediaListResponse getMediaByParentId(UUID parentId) {
        logger.info("Fetching media by parentId={}", parentId);
        List<Media> mediaList;
        if (parentId == null) {
            mediaList = mediaRepository.findAllRootLevelActiveMedia();
        } else {
            mediaList = mediaRepository.findActiveMediaByParentId(parentId);
        }
        List<MediaDetailDto> items = new ArrayList<>();
        for (Media media : mediaList) {
            MediaDetailDto dto = MediaDetailDto.builder()
                    .id(media.getId())
                    .fileName(media.getFileName())
                    .type(media.getType())
                    .parentId(media.getParentId())
                    .filePath(media.getFilePath())
                    .url(media.getUrl())
                    .fileUuid(media.getFileUuid())
                    .createdAt(media.getCreatedAt())
                    .updatedAt(media.getUpdatedAt())
                    .build();
            items.add(dto);
        }
        return MediaListResponse.builder()
                .media(items)
                .totalCount(items.size())
                .build();
    }
}
