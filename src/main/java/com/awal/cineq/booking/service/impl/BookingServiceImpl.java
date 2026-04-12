package com.awal.cineq.booking.service.impl;

import com.awal.cineq.booking.dto.*;
import com.awal.cineq.booking.model.Booking;
import com.awal.cineq.booking.repository.BookingRepository;
import com.awal.cineq.booking.service.BookingService;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.movies.repository.FrontendMovieRepository;
import com.awal.cineq.frontend.screens.repository.FrontendScreenRepository;
import com.awal.cineq.frontend.showtimes.repository.FrontendShowtimeRepository;
import com.awal.cineq.frontend.theatres.repository.FrontendTheatreRepository;
import com.awal.cineq.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final int STATUS_AVAILABLE = 1;

    private final BookingRepository bookingRepository;
    private final FrontendShowtimeRepository showtimeRepository;
    private final FrontendMovieRepository movieRepository;
    private final FrontendTheatreRepository theatreRepository;
    private final FrontendScreenRepository screenRepository;
    private final MongoTemplate mongoTemplate;

    // ─────────────────────────────────────────────────────────────
    // getMyBookings
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<BookingResponse> getMyBookings(String customerId) {
        log.info("getMyBookings — customerId={}", customerId);

        return bookingRepository.findByCustomerId(customerId).stream()
                .sorted(Comparator.comparing(
                        b -> b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .map(this::buildEnrichedResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // getBookingByReference
    // ─────────────────────────────────────────────────────────────

    @Override
    public BookingResponse getBookingByReference(String bookingReference, String customerId) {
        log.info("getBookingByReference — reference={}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!customerId.equals(booking.getCustomerId())) {
            throw new ResourceNotFoundException("Booking not found");
        }

        return buildEnrichedResponse(booking);
    }

    // ─────────────────────────────────────────────────────────────
    // cancelBooking
    // ─────────────────────────────────────────────────────────────

    @Override
    public BookingResponse cancelBooking(String bookingReference, String customerId) {
        log.info("cancelBooking — reference={}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!customerId.equals(booking.getCustomerId())) {
            throw new ResourceNotFoundException("Booking not found");
        }

        if (booking.getDeletedAt() != null) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getSeatStatusCode() != null && booking.getSeatStatusCode() == 2) {
            throw new IllegalStateException("Confirmed bookings cannot be self-cancelled");
        }

        // Soft-delete: removes booking from the unique seat index so seats become available
        booking.setSeatStatusCode(STATUS_AVAILABLE);
        booking.setPaymentStatus(PaymentStatus.FAILED);
        booking.setDeletedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        if (booking.getBookingDetails() != null) {
            booking.getBookingDetails().forEach(d -> d.setSeatStatusCode(STATUS_AVAILABLE));
        }

        return buildEnrichedResponse(bookingRepository.save(booking));
    }

    // ─────────────────────────────────────────────────────────────
    // getBookingsWithFilters
    // ─────────────────────────────────────────────────────────────

    @Override
    public BookingPageResponse getBookingsWithFilters(BookingListFilterRequest filterRequest) {
        log.info("getBookingsWithFilters — filters={}, page={}, size={}, dateRange={} to {}",
                filterRequest, filterRequest.getPage(), filterRequest.getSize(),
                filterRequest.getFromDate(), filterRequest.getToDate());

        // Ensure valid pagination parameters
        int page = filterRequest.getPage() != null ? Math.max(0, filterRequest.getPage()) : 0;
        int size = filterRequest.getSize() != null ? Math.max(1, filterRequest.getSize()) : 10;

        // Build query with optional filters and date range
        Criteria criteria = buildBookingCriteria(filterRequest);

        // Count total matching records
        Query countQuery = new Query(criteria);
        long totalElements = mongoTemplate.count(countQuery, Booking.class);

        // Calculate pagination values
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        // Build and execute query with pagination and sorting
        Query query = new Query(criteria)
                .skip((long) page * size)
                .limit(size)
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"));

        List<Booking> bookings = mongoTemplate.find(query, Booking.class);

        // Map each booking to BookingListDTO with customer information
        List<BookingListDTO> bookingDTOs = bookings.stream()
                .map(this::buildBookingListDTO)
                .collect(Collectors.toList());

        // Build response
        return BookingPageResponse.builder()
                .bookings(bookingDTOs)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Private helper methods
    // ─────────────────────────────────────────────────────────────

    /**
     * Build MongoDB Criteria based on provided filters and date range
     * All filters are optional - if not provided, no filter is applied
     */
    private Criteria buildBookingCriteria(BookingListFilterRequest filterRequest) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Always filter active records (soft-delete)
        criteriaList.add(Criteria.where("deletedAt").is(null));

        // Optional filters
        if (filterRequest.getPaymentStatus() != null) {
            criteriaList.add(Criteria.where("paymentStatus").is(filterRequest.getPaymentStatus()));
        }

        if (filterRequest.getPaymentMethod() != null) {
            criteriaList.add(Criteria.where("paymentMethod").is(filterRequest.getPaymentMethod()));
        }

        if (filterRequest.getSeatStatusCode() != null) {
            criteriaList.add(Criteria.where("seatStatusCode").is(filterRequest.getSeatStatusCode()));
        }

        if (filterRequest.getBookingReference() != null && !filterRequest.getBookingReference().isEmpty()) {
            criteriaList.add(Criteria.where("bookingReference").regex(filterRequest.getBookingReference(), "i"));
        }

        if (filterRequest.getCustomerId() != null && !filterRequest.getCustomerId().isEmpty()) {
            criteriaList.add(Criteria.where("customerId").is(filterRequest.getCustomerId()));
        }

        if (filterRequest.getShowtimeId() != null && !filterRequest.getShowtimeId().isEmpty()) {
            criteriaList.add(Criteria.where("showtimeId").is(filterRequest.getShowtimeId()));
        }

        // Date range filter on updatedAt field
        if (filterRequest.getFromDate() != null || filterRequest.getToDate() != null) {
            Criteria dateCriteria = new Criteria();
            
            if (filterRequest.getFromDate() != null) {
                LocalDateTime startOfDay = filterRequest.getFromDate().atStartOfDay();
                dateCriteria = dateCriteria.gte(startOfDay);
            }
            
            if (filterRequest.getToDate() != null) {
                LocalDateTime endOfDay = filterRequest.getToDate().atTime(LocalTime.MAX);
                dateCriteria = dateCriteria.lte(endOfDay);
            }
            
            criteriaList.add(Criteria.where("updatedAt").andOperator(dateCriteria));
        }

        // Combine all criteria with AND
        if (criteriaList.isEmpty()) {
            return new Criteria();
        }

        return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
    }

    /**
     * Convert Booking model to BookingListDTO with customer details
     */
    private BookingListDTO buildBookingListDTO(Booking booking) {
        // Fetch customer details
        String customerName = null, customerEmail = null, customerPhone = null;
        if (booking.getCustomerId() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = mongoTemplate.findOne(
                    Query.query(Criteria.where("_id").is(booking.getCustomerId())
                            .and("deletedAt").is(null)),
                    Map.class, "customers");
            if (customer != null) {
                String firstName = str(customer, "first_name");
                String lastName = str(customer, "last_name");
                customerName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                customerName = customerName.trim();
                customerEmail = str(customer, "email");
                customerPhone = str(customer, "phone");
            }
        }

        // Resolve seat status name + color
        String statusName = null, statusColor = null;
        if (booking.getSeatStatusCode() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> seatStatus = mongoTemplate.findOne(
                    Query.query(Criteria.where("code").is(booking.getSeatStatusCode())
                            .and("isActive").is(true)
                            .and("deletedAt").is(null)),
                    Map.class, "seat_statuses");
            if (seatStatus != null) {
                statusName = str(seatStatus, "name");
                statusColor = str(seatStatus, "color");
            }
        }

        // Map booking details
        final String resolvedStatusName = statusName;
        final String resolvedStatusColor = statusColor;
        List<BookingDetailResponse> detailResponses = null;
        if (booking.getBookingDetails() != null) {
            detailResponses = booking.getBookingDetails().stream()
                    .map(d -> BookingDetailResponse.builder()
                            .seatName(d.getSeatName())
                            .row(d.getRow())
                            .col(d.getCol())
                            .seatCode(d.getSeatCode())
                            .seatPrice(d.getSeatPrice() != null ? d.getSeatPrice().doubleValue() : null)
                            .seatStatusCode(d.getSeatStatusCode())
                            .seatStatusName(resolvedStatusName)
                            .seatStatusColor(resolvedStatusColor)
                            .build())
                    .collect(Collectors.toList());
        }

        // Compute status string for UI
        String computedStatus = computeBookingStatus(booking);

        BookingListDTO.BookingListDTOBuilder builder = BookingListDTO.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .showtimeId(booking.getShowtimeId())
                .customer(CustomerDTO.builder()
                        .id(booking.getCustomerId())
                        .name(customerName)
                        .email(customerEmail)
                        .phone(customerPhone)
                        .build())
                .numberOfSeats(booking.getNumberOfSeats())
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount().doubleValue() : null)
                .bookingDate(booking.getBookingDate())
                .seatStatusCode(booking.getSeatStatusCode())
                .seatStatusName(statusName)
                .seatStatusColor(statusColor)
                .paymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : null)
                .paymentMethod(booking.getPaymentMethod() != null ? booking.getPaymentMethod().name() : null)
                .paymentReference(booking.getPaymentReference())
                .bookingDetails(detailResponses)
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .expiresAt(booking.getExpiresAt())
                .deletedAt(booking.getDeletedAt())
                .status(computedStatus);

        // Enrich with showtime → movie → theatre → screen data
        if (booking.getShowtimeId() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> showtime = showtimeRepository.findByIdAndActive(booking.getShowtimeId());
                if (showtime != null) {
                    String movieId = str(showtime, "movieId");
                    String theatreId = str(showtime, "theatreId");
                    String screenId = str(showtime, "screenId");

                    builder.movieId(movieId)
                            .theatreId(theatreId)
                            .screenId(screenId)
                            .showDate(str(showtime, "showDate"))
                            .showTime(str(showtime, "showTime"))
                            .language(str(showtime, "language"))
                            .format(str(showtime, "format"));

                    enrichMovie(builder, movieId);
                    enrichTheatre(builder, theatreId);
                    enrichScreen(builder, screenId);
                }
            } catch (Exception e) {
                log.warn("Could not fetch showtime data for showtimeId={}: {}", booking.getShowtimeId(), e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * Compute booking status for UI display
     */
    private String computeBookingStatus(Booking booking) {
        if (booking.getDeletedAt() != null) {
            return "Cancelled";
        }
        if (booking.getSeatStatusCode() != null && booking.getSeatStatusCode() == 2) {
            return "Confirmed";
        }
        if (booking.getSeatStatusCode() != null && booking.getSeatStatusCode() == 3) {
            return "Pending";
        }
        return "Unknown";
    }

    private void enrichMovie(BookingListDTO.BookingListDTOBuilder builder, String movieId) {
        if (movieId == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> movie = movieRepository.findByIdAndActive(movieId);
            if (movie != null) {
                builder.movieTitle(str(movie, "title")).moviePoster(str(movie, "poster"));
            }
        } catch (Exception e) {
            log.warn("Could not fetch movie {}: {}", movieId, e.getMessage());
        }
    }

    private void enrichTheatre(BookingListDTO.BookingListDTOBuilder builder, String theatreId) {
        if (theatreId == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> theatre = theatreRepository.findByIdAndActive(theatreId);
            if (theatre != null) builder.theatreName(str(theatre, "name"));
        } catch (Exception e) {
            log.warn("Could not fetch theatre {}: {}", theatreId, e.getMessage());
        }
    }

    private void enrichScreen(BookingListDTO.BookingListDTOBuilder builder, String screenId) {
        if (screenId == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> screen = screenRepository.findByIdAndActive(screenId);
            if (screen != null) builder.screenName(str(screen, "screenName"));
        } catch (Exception e) {
            log.warn("Could not fetch screen {}: {}", screenId, e.getMessage());
        }
    }

    private BookingResponse buildEnrichedResponse(Booking booking) {
        // Resolve seat status name + color from seat_statuses collection
        String statusName = null, statusColor = null;
        if (booking.getSeatStatusCode() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> seatStatus = mongoTemplate.findOne(
                    Query.query(Criteria.where("code").is(booking.getSeatStatusCode())
                            .and("isActive").is(true)
                            .and("deletedAt").is(null)),
                    Map.class, "seat_statuses");
            if (seatStatus != null) {
                statusName  = str(seatStatus, "name");
                statusColor = str(seatStatus, "color");
            }
        }

        // Map booking details
        final String resolvedStatusName  = statusName;
        final String resolvedStatusColor = statusColor;
        List<BookingDetailResponse> detailResponses = null;
        if (booking.getBookingDetails() != null) {
            detailResponses = booking.getBookingDetails().stream()
                    .map(d -> BookingDetailResponse.builder()
                            .seatName(d.getSeatName())
                            .row(d.getRow())
                            .col(d.getCol())
                            .seatCode(d.getSeatCode())
                            .seatPrice(d.getSeatPrice() != null ? d.getSeatPrice().doubleValue() : null)
                            .seatStatusCode(d.getSeatStatusCode())
                            .seatStatusName(resolvedStatusName)
                            .seatStatusColor(resolvedStatusColor)
                            .build())
                    .collect(Collectors.toList());
        }

        BookingResponse.BookingResponseBuilder builder = BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .showtimeId(booking.getShowtimeId())
                .customerId(booking.getCustomerId())
                .bookingDate(booking.getBookingDate() != null ? booking.getBookingDate().toString() : null)
                .numberOfSeats(booking.getNumberOfSeats())
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount().doubleValue() : null)
                .seatStatusCode(booking.getSeatStatusCode())
                .seatStatusName(statusName)
                .seatStatusColor(statusColor)
                .paymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : null)
                .paymentMethod(booking.getPaymentMethod() != null ? booking.getPaymentMethod().name() : null)
                .paymentReference(booking.getPaymentReference())
                .expiresAt(booking.getExpiresAt() != null ? booking.getExpiresAt().toString() : null)
                .bookingDetails(detailResponses);

        // Enrich with showtime → movie → theatre → screen data
        if (booking.getShowtimeId() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> showtime = showtimeRepository.findByIdAndActive(booking.getShowtimeId());
                if (showtime != null) {
                    String movieId   = str(showtime, "movieId");
                    String theatreId = str(showtime, "theatreId");
                    String screenId  = str(showtime, "screenId");

                    builder.movieId(movieId).theatreId(theatreId).screenId(screenId)
                           .showDate(str(showtime, "showDate"))
                           .showTime(str(showtime, "showTime"))
                           .language(str(showtime, "language"))
                           .format(str(showtime, "format"));

                    enrichMovie(builder, movieId);
                    enrichTheatre(builder, theatreId);
                    enrichScreen(builder, screenId);
                }
            } catch (Exception e) {
                log.warn("Could not fetch showtime data for showtimeId={}: {}", booking.getShowtimeId(), e.getMessage());
            }
        }

        return builder.build();
    }

    private void enrichMovie(BookingResponse.BookingResponseBuilder builder, String movieId) {
        if (movieId == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> movie = movieRepository.findByIdAndActive(movieId);
            if (movie != null) {
                builder.movieTitle(str(movie, "title")).moviePoster(str(movie, "poster"));
            }
        } catch (Exception e) {
            log.warn("Could not fetch movie {}: {}", movieId, e.getMessage());
        }
    }

    private void enrichTheatre(BookingResponse.BookingResponseBuilder builder, String theatreId) {
        if (theatreId == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> theatre = theatreRepository.findByIdAndActive(theatreId);
            if (theatre != null) builder.theatreName(str(theatre, "name"));
        } catch (Exception e) {
            log.warn("Could not fetch theatre {}: {}", theatreId, e.getMessage());
        }
    }

    private void enrichScreen(BookingResponse.BookingResponseBuilder builder, String screenId) {
        if (screenId == null) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> screen = screenRepository.findByIdAndActive(screenId);
            if (screen != null) builder.screenName(str(screen, "screenName"));
        } catch (Exception e) {
            log.warn("Could not fetch screen {}: {}", screenId, e.getMessage());
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
