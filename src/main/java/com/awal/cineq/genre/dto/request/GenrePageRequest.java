package com.awal.cineq.genre.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class GenrePageRequest {

    public static final String DEFAULT_SORT_BY = "name";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    @Min(value = 1, message = "Page must be greater than or equal to 1")
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

    // Utility methods
    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
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

    public PageRequest toPageRequest() {
        int pageIndex = (page != null && page > 0) ? page - 1 : 0;
        return PageRequest.of(pageIndex, size, isAscending() ? ASC : DESC, sortBy);
    }
}
