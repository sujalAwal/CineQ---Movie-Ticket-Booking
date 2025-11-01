package com.awal.cineq.media.service;

import com.awal.cineq.media.dto.request.MediaDeleteRequestDto;
import com.awal.cineq.media.dto.request.FolderCreateRequest;
import com.awal.cineq.media.dto.request.MediaUploadRequestDto;
import com.awal.cineq.media.dto.response.MediaResponse;
import com.awal.cineq.media.dto.response.MediaListResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MediaService {

    public MediaResponse uploadMultipleFiles(MediaUploadRequestDto requestDto);

    // Delete multiple media records by their primary UUIDs. Returns per-id results (status/message).
    public List<Map<String, Object>> deleteMultipleFiles(MediaDeleteRequestDto mediaIds);

    // Delete a single media record by its primary MediaDeleteRequestDto and return a per-id result map.
    public Map<String, Object> deleteSingleFile(MediaDeleteRequestDto mediaId);

    // Create a folder in the media system. Returns the created folder information.
    public MediaResponse createFolder(FolderCreateRequest request);

    // Get all files and folders by parent ID. If parentId is null, returns root level items.
    public MediaListResponse getMediaByParentId(UUID parentId);

    // Get all active folders (flat list)
    public MediaListResponse getAllFolders();
}
