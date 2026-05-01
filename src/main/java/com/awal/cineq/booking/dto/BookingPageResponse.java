package com.awal.cineq.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response wrapper for booking listings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPageResponse {

    private List<BookingListDTO> bookings;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
