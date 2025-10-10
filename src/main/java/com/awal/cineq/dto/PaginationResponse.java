package com.awal.cineq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(exclude = "data")
public class PaginationResponse<T> {
    private boolean success;
    private String message;
    private List<T> data;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;
    private boolean hasPrevious;

    public PaginationResponse() {}

    public PaginationResponse(boolean success, String message, List<T> data, int page, int size, int totalPages, long totalElements, boolean hasNext, boolean hasPrevious) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public static <T> PaginationResponse<T> of(boolean success, String message, List<T> data, int page, int size, int totalPages, long totalElements, boolean hasNext, boolean hasPrevious) {
        return new PaginationResponse<>(success, message, data, page, size, totalPages, totalElements, hasNext, hasPrevious);
    }
    public static <T> PaginationResponse<T> success( String message, List<T> data, int page, int size, int totalPages, long totalElements, boolean hasNext, boolean hasPrevious) {
        return new PaginationResponse<>(true, message, data, page, size, totalPages, totalElements, hasNext, hasPrevious);
    }
}
