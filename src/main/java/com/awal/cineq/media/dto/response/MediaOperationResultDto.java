package com.awal.cineq.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing the result of a media operation (e.g., delete).
 * Provides structured information about operation success or failure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaOperationResultDto {

    private UUID id;
    private OperationStatus status;
    private String message;
    private String deleteFromStorage;
    private String storageMessage;
    private Boolean fileDeleted;

    /**
     * Enum representing the status of a media operation
     */
    public enum OperationStatus {
        SUCCESS,
        DELETED,
        FAILED,
        NOT_FOUND,
        PARTIAL_SUCCESS
    }

    /**
     * Converts this DTO to a Map representation for backward compatibility.
     * @deprecated Use the DTO directly instead of Map
     */
    @Deprecated
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (id != null) map.put("id", id);
        if (status != null) map.put("status", status.name());
        if (message != null) map.put("message", message);
        if (deleteFromStorage != null) map.put("deleteFromStorage", deleteFromStorage);
        if (storageMessage != null) map.put("storageMessage", storageMessage);
        if (fileDeleted != null) map.put("fileDeleted", fileDeleted);
        return map;
    }

    // Convenience factory methods
    public static MediaOperationResultDto success(UUID id) {
        return MediaOperationResultDto.builder()
                .id(id)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    public static MediaOperationResultDto deleted(UUID id) {
        return MediaOperationResultDto.builder()
                .id(id)
                .status(OperationStatus.DELETED)
                .build();
    }

    public static MediaOperationResultDto failed(UUID id, String message) {
        return MediaOperationResultDto.builder()
                .id(id)
                .status(OperationStatus.FAILED)
                .message(message)
                .build();
    }

    public static MediaOperationResultDto notFound(UUID id, String message) {
        return MediaOperationResultDto.builder()
                .id(id)
                .status(OperationStatus.NOT_FOUND)
                .message(message)
                .build();
    }
}
