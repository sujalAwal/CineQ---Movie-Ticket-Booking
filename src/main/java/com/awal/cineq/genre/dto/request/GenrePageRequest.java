package com.awal.cineq.genre.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenrePageRequest {

    public static final String DEFAULT_SORT_BY = "name";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    @JsonProperty("page")
    private Integer page = DEFAULT_PAGE;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    @Max(value = MAX_SIZE, message = "Size must be less than or equal to " + MAX_SIZE)
    @JsonProperty("size")
    private Integer size = DEFAULT_SIZE;

    @Pattern(regexp = "name|createdAt|updatedAt", message = "Sort by must be one of: name, createdAt, updatedAt")
    @JsonProperty("sortBy")
    private String sortBy = DEFAULT_SORT_BY;

    @Pattern(regexp = "asc|desc", message = "Sort direction must be either 'asc' or 'desc'")
    @JsonProperty("sortDirection")
    private String sortDirection = DEFAULT_SORT_DIRECTION;

    @JsonProperty("search")
    private String search;

    @JsonProperty("active")
    private Boolean active;

    // Default constructor
    public GenrePageRequest() {
    }

    // Getters and Setters
    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // Utility methods
    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
    }

    public boolean hasActiveFilter() {
        return active != null;
    }

    public boolean isAscending() {
        return "asc".equalsIgnoreCase(sortDirection);
    }

    /**
     * Calculates the offset for database queries
     */
    public int getOffset() {
        return page * size;
    }

    @Override
    public String toString() {
        return "GenrePageRequest{" +
                "page=" + page +
                ", size=" + size +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                ", search='" + search + '\'' +
                ", active=" + active +
                '}';
    }
}
