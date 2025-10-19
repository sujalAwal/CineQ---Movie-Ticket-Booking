package com.awal.cineq.media.service.impl;

import com.awal.cineq.client.supabase.config.SupabaseConfig;
import com.awal.cineq.exception.BusinessException;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.media.config.Storage;
import com.awal.cineq.media.dto.request.MediaDeleteRequestDto;
import com.awal.cineq.media.dto.request.FolderCreateRequest;
import com.awal.cineq.media.dto.request.MediaUploadRequestDto;
import com.awal.cineq.media.dto.response.MediaResponse;
import com.awal.cineq.media.dto.response.SupabaseResponse;
import com.awal.cineq.media.dto.response.MediaItemDto;
import com.awal.cineq.media.dto.response.MediaOperationResultDto;
import com.awal.cineq.media.dto.response.MediaListResponse;
import com.awal.cineq.media.dto.response.MediaDetailDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.awal.cineq.media.config.StorageType;
import com.awal.cineq.media.model.Media;
import com.awal.cineq.media.model.MediaType;
import com.awal.cineq.media.repository.MediaRepository;
import com.awal.cineq.media.service.MediaService;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service("supabaseMediaService")
@AllArgsConstructor
public class SupabaseMediaService implements MediaService {

    private final MediaRepository mediaRepository;
    private final SupabaseConfig storageConfig;
    private final WebClient webClient;
    private static final Logger logger = LoggerFactory.getLogger(SupabaseMediaService.class);
    private final Storage uploadDir;

    public List<Map<String, Object>> deleteMultipleFiles(MediaDeleteRequestDto mediaRequestDto) {
        logger.info("Start: deleteMultipleFiles, count={}", mediaRequestDto == null || mediaRequestDto.mediaIds == null ? 0 : mediaRequestDto.mediaIds.size());
        List<Map<String, Object>> results = new ArrayList<>();

        if (mediaRequestDto == null || mediaRequestDto.mediaIds == null || mediaRequestDto.mediaIds.isEmpty()) {
            throw new BusinessException("No media delete requests provided for deletion");
        }

        for (UUID id : mediaRequestDto.mediaIds) {
            MediaDeleteRequestDto singleDto = new MediaDeleteRequestDto();
            singleDto.mediaIds = java.util.Collections.singletonList(id);
            results.add(deleteSingleFile(singleDto));
        }

        logger.info("End: deleteMultipleFiles, processed={}", results.size());
        return results;
    }

    // Delete a single media by MediaDeleteRequestDto. Returns a map describing the outcome for this id.
    public Map<String, Object> deleteSingleFile(MediaDeleteRequestDto mediaDeleteRequestDto) {
        logger.info("Start: deleteSingleFile (Supabase)");

        if (mediaDeleteRequestDto == null || mediaDeleteRequestDto.mediaIds == null || mediaDeleteRequestDto.mediaIds.isEmpty()) {
            logger.warn("No mediaId provided in delete request");
            return MediaOperationResultDto.failed(null, "No mediaId provided in request").toMap();
        }

        UUID mediaId = mediaDeleteRequestDto.mediaIds.get(0);
        logger.info("Processing delete for mediaId={}", mediaId);

        if (mediaId == null) {
            logger.warn("Null mediaId provided");
            return MediaOperationResultDto.failed(null, "Null id provided").toMap();
        }

        try {
            Optional<Media> mediaOpt = mediaRepository.findById(mediaId);
            if (mediaOpt.isEmpty()) {
                logger.warn("Media not found with id={}", mediaId);
                return MediaOperationResultDto.notFound(mediaId, "Media not found").toMap();
            }

            Media media = mediaOpt.get();
            logger.info("Found media: id={}, type={}, filePath={}", mediaId, media.getType(), media.getFilePath());

            // Prefer stored filePath (key) for deletion; fall back to URL when necessary
            String pathOrUrl = media.getFilePath() != null ? media.getFilePath() : media.getUrl();

            MediaOperationResultDto.MediaOperationResultDtoBuilder resultBuilder = MediaOperationResultDto.builder()
                    .id(mediaId);

            try {
                deleteFromSupabase(pathOrUrl);
                logger.info("Successfully deleted from Supabase storage: {}", pathOrUrl);
            } catch (Exception ex) {
                // Log but continue to soft-delete the record (user might want record removed regardless)
                logger.warn("Failed to delete from Supabase for media id {}: {}", mediaId, ex.getMessage());
                resultBuilder.deleteFromStorage("FAILED")
                           .storageMessage(ex.getMessage());
            }

            // Soft delete in DB
            media.setDeletedAt(LocalDateTime.now());
            media.setActive(false);
            mediaRepository.save(media);
            logger.info("Media soft-deleted in database: id={}", mediaId);

            resultBuilder.status(MediaOperationResultDto.OperationStatus.DELETED);
            return resultBuilder.build().toMap();

        } catch (Exception e) {
            logger.error("Error deleting media id {}: {}", mediaId, e.getMessage());
            return MediaOperationResultDto.failed(mediaId, e.getMessage()).toMap();
        }
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
                String filePath = (parentId == null)
                        ? storageConfig.getBucket() + "/" + fileTitle
                        : storageConfig.getBucket() + "/" + getParentPath(parentId) + "/" + fileTitle;
                if (!mediaRepository.findByFilePath(filePath).isEmpty()) {
                    throw new BusinessException("File with the same name already exists: " + fileTitle);
                }
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
            } catch (BusinessException be) {
                logger.error("Business error uploading file: {} - {}", file.getOriginalFilename(), be.getMessage());
                throw be;
            } catch (Exception e) {
                logger.error("Error uploading file: {} - {}", file.getOriginalFilename(), e.getMessage());
                String fname = file != null ? file.getOriginalFilename() : "<unknown>";
                throw new BusinessException("Failed to upload file: " + fname, e);
            }
        }

        logger.info("End: uploadMultipleFiles, uploadedFilesCount={}", uploadedFiles.size());
        return new MediaResponse(uploadedFiles);
    }

    private String getParentPath(UUID parentId) {
        Optional<Media> parentMediaOpt = mediaRepository.findById(parentId);
        if (parentMediaOpt.isPresent()) {
            Media parentMedia = parentMediaOpt.get();
            return parentMedia.getFilePath();
        } else {
            throw new BusinessException("Parent media not found with id: " + parentId);
        }
    }


    public Media uploadSingleFile(MultipartFile file, String title, UUID parentId) {
        try {
        validateFile(file);

        SupabaseResponse supabaseResponse = uploadToSupabase(file, file.getOriginalFilename());
        String signedUrl = null;
        signedUrl = getSignedUrl(supabaseResponse != null ? supabaseResponse.getKey() : null, supabaseResponse != null ? supabaseResponse.getId() : null);
        MediaType mediaType = determineMediaType(file.getContentType());

        logger.info("fileUrl: {}", supabaseResponse != null ? supabaseResponse.getId() : null); // Debug here
        Media media = createMediaEntity(title, mediaType, parentId, supabaseResponse, signedUrl);
        return mediaRepository.save(media);
        } catch (Exception e) {
            logger.error("Error in uploadSingleFile: {}", e.getMessage());
            throw new BusinessException("File upload failed: " + e.getMessage(), e);
        }
    }

    public void deleteFile(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + mediaId));

        deleteFromSupabase(media.getUrl());

        // Soft delete
        media.setDeletedAt(LocalDateTime.now());
        mediaRepository.save(media);
    }

    // Create a folder in the media system without calling Supabase API.
    // Only stores folder metadata in the database.
    @Override
    public MediaResponse createFolder(FolderCreateRequest request) {
        logger.info("Start: createFolder (Supabase), folderName={}, parentId={}", request.getName(), request.getParentId());

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

            // Build folder path
            String folderPath;
            if (request.getParentId() != null) {
                String parentPath = getParentPath(request.getParentId());
                folderPath = parentPath + "/" + request.getName().trim();
                logger.info("Folder path with parent: {}", folderPath);
            } else {
                folderPath = storageConfig.getBucket() + "/" + request.getName().trim();
                logger.info("Folder path at root: {}", folderPath);
            }

            String filePath = (request.getParentId() == null)
                    ? storageConfig.getBucket() + "/" + request.getName().trim()
                    : storageConfig.getBucket() + "/" + getParentPath(request.getParentId()) + "/" + request.getName().trim();
            if (!mediaRepository.findByFilePath(filePath).isEmpty()) {
                throw new BusinessException("File with the same name already exists: " + request.getName());
            }

            // Create folder entity
            Media folder = new Media();
            folder.setFileName(request.getName().trim());
            folder.setType(MediaType.FOLDER);
            folder.setActive(true);
            folder.setParentId(request.getParentId());
            folder.setFilePath(folderPath);
            folder.setCreatedAt(LocalDateTime.now());
            folder.setUpdatedAt(LocalDateTime.now());

            // Save folder to database (no Supabase API call needed)
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
                    .build();

            List<Map<String, Object>> result = new ArrayList<>();
            result.add(folderInfo.toMap());

            logger.info("End: createFolder (Supabase), folderId={}", savedFolder.getId());
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
        List<MediaDetailDto> items =buildHierarchicalMedia(mediaList, parentId);
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

    public StorageType getStorageType() {
        return StorageType.SUPABASE;
    }
    // ✅ ADD THIS NEW METHOD
    private List<MediaDetailDto> buildHierarchicalMedia(List<Media> allMedia, UUID parentId) {
        // Convert all media to DTOs and store in map for quick access
        Map<UUID, MediaDetailDto> dtoMap = allMedia.stream()
                .map(this::convertToDto)
                .collect(Collectors.toMap(MediaDetailDto::getId, Function.identity()));

        // Build parent-child relationships
        for (MediaDetailDto dto : dtoMap.values()) {
            if (dto.getParentId() != null && dtoMap.containsKey(dto.getParentId())) {
                MediaDetailDto parent = dtoMap.get(dto.getParentId());

                // Initialize children list if null
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }

                // Add current dto to parent's children
                parent.getChildren().add(dto);
            }
        }

        // Return requested level
        if (parentId == null) {
            // Return all root level items (parentId is null)
            return dtoMap.values().stream()
                    .filter(dto -> dto.getParentId() == null)
                    .collect(Collectors.toList());
        } else {
            // Return children of specific parent
            MediaDetailDto parentDto = dtoMap.get(parentId);
            return parentDto != null && parentDto.getChildren() != null ?
                    parentDto.getChildren() : new ArrayList<>();
        }
    }
    private MediaDetailDto convertToDto(Media media) {
        return MediaDetailDto.builder()
                .id(media.getId())
                .fileName(media.getFileName())
                .type(media.getType())
                .parentId(media.getParentId())
                .filePath(media.getFilePath())
                .url(media.getUrl())
                .fileUuid(media.getFileUuid())
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .children(new ArrayList<>()) // Initialize empty children list
                .build();
    }

    // Private helper methods - Supabase specific
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        if (file.getSize() > uploadDir.getMaxFileSize() ) { // for Supabase
            throw new RuntimeException("File size exceeds the limit");
        }
    }

    private SupabaseResponse uploadToSupabase(MultipartFile file, String fileName) {
        try {


            String url = storageConfig.getApiUrl()+storageConfig.getBucket()+"/"+fileName;
       var result = this.webClient.post().uri(url)
                    .header("Authorization", "Bearer " + storageConfig.getApiKey())
                    .header("Content-Type", file.getContentType())
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));
            ObjectMapper mapper = new ObjectMapper();
            SupabaseResponse response = mapper.readValue(result, SupabaseResponse.class);
       logger.info(response.toString());
            return response;

        } catch (Exception ex) {
            logger.error("Supabase upload failed: {}", ex.getMessage());
            throw new RuntimeException("Supabase upload failed: " + ex.getMessage(), ex);
        }
    }
    private String getSignedUrl(String path, String id) {
        try {

            String url = storageConfig.getApiUrl() + "sign/" + path;

            Map<String, Object> payload = Collections.singletonMap("expiresIn", 978654312);

            Map<String, Object> response = this.webClient.post().uri(url)
                    .header("Authorization", "Bearer " + storageConfig.getApiKey())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(sts -> sts.isError(), resp -> resp.bodyToMono(String.class).flatMap(body -> {
                        logger.error("Supabase sign error response: {}", body);
                        if (resp.statusCode() == HttpStatus.UNAUTHORIZED) {
                            return Mono.<Throwable>error(new BusinessException("Unauthorized: invalid Supabase credentials or access denied"));
                        }
                        return Mono.<Throwable>error(new BusinessException("Supabase sign failed: " + body));
                    }))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(15));

            logger.info("Signed URL response: {}", response);

            if (response == null || response.isEmpty()) {
                throw new BusinessException("Empty response from Supabase sign API");
            }

            // Common keys that Supabase or other signing endpoints may return
            Object candidate = null;
            candidate = firstNonNull(response.get("signedUrl"), response.get("signed_url"), response.get("signedURL"));

            if (candidate != null) {
                return candidate.toString();
            }

            throw new BusinessException("No signed signURL found in Supabase response: " + response);

        } catch (BusinessException be) {
            logger.error("Supabase sign unauthorized: {}", be.getMessage());
            throw be;
        } catch (Exception ex) {
            logger.error("Supabase sign failed: {}", ex.getMessage(), ex);
            throw new BusinessException("Supabase sign failed: " + ex.getMessage(), ex);
        }
    }

    // small helper used above
    private Object firstNonNull(Object... vals) {
        for (Object v : vals) {
            if (v != null) return v;
        }
        return null;
    }

    private void deleteFromSupabase(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                logger.warn("No file path or url supplied to deleteFromSupabase");
                return;
            }

            // If the caller passed a full URL, extract the path/filename. If they passed a key/filePath,
            // use it directly. Supabase storage delete endpoints usually target the bucket + path.
            String keyOrFileName;
            if (fileUrl.startsWith("http")) {
                keyOrFileName = extractFileNameFromUrl(fileUrl);
            } else {
                keyOrFileName = fileUrl;
            }

            String url = storageConfig.getApiUrl() + keyOrFileName;

            this.webClient.delete().uri(url)
                    .header("Authorization", "Bearer " + storageConfig.getApiKey())
                    .retrieve()
                    .onStatus(sts -> sts.isError(), resp -> resp.bodyToMono(String.class).flatMap(body -> {
                        logger.error("Supabase delete error response: {}", body);
                        return Mono.<Throwable>error(new BusinessException("Supabase delete failed: " + body));
                    }))
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(15));

            logger.info("Deleted from Supabase: {}", keyOrFileName);

        } catch (BusinessException be) {
            logger.error("Supabase deletion business error: {}", be.getMessage());
            throw be;
        } catch (Exception e) {
            logger.error("Supabase deletion failed: {}", e.getMessage(), e);
            throw new RuntimeException("Supabase deletion failed: " + e.getMessage(), e);
        }
    }

    private String generateFileName(MultipartFile file, UUID mediaId) {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        return mediaId + "." + fileExtension;
    }

    private String extractFileNameFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType == null) return MediaType.DOCUMENT;
        if (contentType.startsWith("image/")) return MediaType.IMAGE;
        if (contentType.startsWith("video/")) return MediaType.VIDEO;
        if (contentType.startsWith("audio/")) return MediaType.AUDIO;
        return MediaType.DOCUMENT;
    }

    private Media createMediaEntity(String title, MediaType type, UUID parentId, SupabaseResponse response, String signedUrl) {
        Media media = new Media();
        media.setFileName(title);

        // Prefer the signed URL returned by Supabase sign API if available
        if (signedUrl != null && !signedUrl.trim().isEmpty()) {
            media.setUrl(signedUrl);
        }
        media.setType(type);
        media.setActive(true);
        media.setParentId(parentId);
        media.setFilePath(response != null ?response.getKey() : null);
        media.setFileUuid(response != null ?response.getId() : null);
        media.setCreatedAt(LocalDateTime.now());
        media.setUpdatedAt(LocalDateTime.now());
        return media;
    }
}
