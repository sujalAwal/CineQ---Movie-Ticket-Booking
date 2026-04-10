package com.awal.cineq.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {

    // Core booking fields
    private String id;
    private String bookingReference;
    private String showtimeId;
    private String customerId;
    private String bookingDate;       // ISO format string
    private Integer numberOfSeats;
    private Double totalAmount;
    private String bookingStatus;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;
    private List<BookingDetailResponse> bookingDetails;

    // Enriched showtime fields
    private String movieId;
    private String movieTitle;
    private String moviePoster;
    private String theatreId;
    private String theatreName;
    private String screenId;
    private String screenName;
    private String showDate;
    private String showTime;
    private String language;
    private String format;
}
