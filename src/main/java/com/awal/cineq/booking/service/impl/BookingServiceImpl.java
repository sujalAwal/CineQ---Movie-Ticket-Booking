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
import org.bson.types.ObjectId;
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

        List<Booking> bookings = bookingRepository.findByCustomerId(customerId);
        bookings.sort(Comparator.comparing(
            b -> b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN,
            Comparator.reverseOrder()));

        EnrichmentContext context = buildEnrichmentContext(bookings, false);

        return bookings.stream()
            .map(booking -> buildEnrichedResponse(booking, context))
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
        EnrichmentContext context = buildEnrichmentContext(bookings, true);

        // Map each booking to BookingListDTO with customer information
        List<BookingListDTO> bookingDTOs = bookings.stream()
            .map(booking -> buildBookingListDTO(booking, context))
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
    private BookingListDTO buildBookingListDTO(Booking booking, EnrichmentContext context) {
        // Resolve customer details from preloaded map
        String customerName = null, customerEmail = null, customerPhone = null;
        if (booking.getCustomerId() != null) {
            Map<String, Object> customer = context.customersById().get(booking.getCustomerId());
            if (customer != null) {
                String firstName = str(customer, "first_name");
                if (firstName == null) firstName = str(customer, "firstName");

                String lastName = str(customer, "last_name");
                if (lastName == null) lastName = str(customer, "lastName");

                customerName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                customerEmail = str(customer, "email");
                customerPhone = str(customer, "phone");
            }
        }

        // Resolve seat status from preloaded map
        String statusName = null, statusColor = null;
        if (booking.getSeatStatusCode() != null) {
            Map<String, Object> seatStatus = context.seatStatusesByCode().get(booking.getSeatStatusCode());
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

        // Enrich with showtime → movie → theatre → screen data from preloaded maps
        if (booking.getShowtimeId() != null) {
            Map<String, Object> showtime = context.showtimesById().get(booking.getShowtimeId());
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

                enrichMovie(builder, movieId, context);
                enrichTheatre(builder, theatreId, context);
                enrichScreen(builder, screenId, context);
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

    private void enrichMovie(BookingListDTO.BookingListDTOBuilder builder, String movieId, EnrichmentContext context) {
        if (movieId == null) return;
        Map<String, Object> movie = context.moviesById().get(movieId);
        if (movie != null) {
            builder.movieTitle(str(movie, "title")).moviePoster(str(movie, "poster"));
        }
    }

    private void enrichTheatre(BookingListDTO.BookingListDTOBuilder builder, String theatreId, EnrichmentContext context) {
        if (theatreId == null) return;
        Map<String, Object> theatre = context.theatresById().get(theatreId);
        if (theatre != null) {
            builder.theatreName(str(theatre, "name"));
        }
    }

    private void enrichScreen(BookingListDTO.BookingListDTOBuilder builder, String screenId, EnrichmentContext context) {
        if (screenId == null) return;
        Map<String, Object> screen = context.screensById().get(screenId);
        if (screen != null) {
            builder.screenName(str(screen, "screenName"));
        }
    }

    private BookingResponse buildEnrichedResponse(Booking booking) {
        EnrichmentContext context = buildEnrichmentContext(Collections.singletonList(booking), false);
        return buildEnrichedResponse(booking, context);
    }

    private BookingResponse buildEnrichedResponse(Booking booking, EnrichmentContext context) {
        // Resolve seat status name + color from preloaded map
        String statusName = null, statusColor = null;
        if (booking.getSeatStatusCode() != null) {
            Map<String, Object> seatStatus = context.seatStatusesByCode().get(booking.getSeatStatusCode());
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

        // Enrich with showtime → movie → theatre → screen data from preloaded maps
        if (booking.getShowtimeId() != null) {
            Map<String, Object> showtime = context.showtimesById().get(booking.getShowtimeId());
            if (showtime != null) {
                String movieId = str(showtime, "movieId");
                String theatreId = str(showtime, "theatreId");
                String screenId = str(showtime, "screenId");

                builder.movieId(movieId).theatreId(theatreId).screenId(screenId)
                        .showDate(str(showtime, "showDate"))
                        .showTime(str(showtime, "showTime"))
                        .language(str(showtime, "language"))
                        .format(str(showtime, "format"));

                enrichMovie(builder, movieId, context);
                enrichTheatre(builder, theatreId, context);
                enrichScreen(builder, screenId, context);
            }
        }

        return builder.build();
    }

    private void enrichMovie(BookingResponse.BookingResponseBuilder builder, String movieId, EnrichmentContext context) {
        if (movieId == null) return;
        Map<String, Object> movie = context.moviesById().get(movieId);
        if (movie != null) {
            builder.movieTitle(str(movie, "title")).moviePoster(str(movie, "poster"));
        }
    }

    private void enrichTheatre(BookingResponse.BookingResponseBuilder builder, String theatreId, EnrichmentContext context) {
        if (theatreId == null) return;
        Map<String, Object> theatre = context.theatresById().get(theatreId);
        if (theatre != null) {
            builder.theatreName(str(theatre, "name"));
        }
    }

    private void enrichScreen(BookingResponse.BookingResponseBuilder builder, String screenId, EnrichmentContext context) {
        if (screenId == null) return;
        Map<String, Object> screen = context.screensById().get(screenId);
        if (screen != null) {
            builder.screenName(str(screen, "screenName"));
        }
    }

    private EnrichmentContext buildEnrichmentContext(List<Booking> bookings, boolean includeCustomers) {
        if (bookings == null || bookings.isEmpty()) {
            return EnrichmentContext.empty();
        }

        Set<Integer> seatStatusCodes = new HashSet<>();
        Set<String> customerIds = new HashSet<>();
        Set<String> showtimeIds = new HashSet<>();

        for (Booking booking : bookings) {
            if (booking.getSeatStatusCode() != null) {
                seatStatusCodes.add(booking.getSeatStatusCode());
            }
            addId(customerIds, booking.getCustomerId());
            addId(showtimeIds, booking.getShowtimeId());
        }

        Map<Integer, Map<String, Object>> seatStatusesByCode = loadSeatStatusesByCode(seatStatusCodes);
        Map<String, Map<String, Object>> customersById = includeCustomers
                ? loadDocumentsById("customers", customerIds, false)
                : Collections.emptyMap();
        Map<String, Map<String, Object>> showtimesById = loadDocumentsById("showtimes", showtimeIds, true);

        Set<String> movieIds = new HashSet<>();
        Set<String> theatreIds = new HashSet<>();
        Set<String> screenIds = new HashSet<>();

        for (Map<String, Object> showtime : showtimesById.values()) {
            addId(movieIds, str(showtime, "movieId"));
            addId(theatreIds, str(showtime, "theatreId"));
            addId(screenIds, str(showtime, "screenId"));
        }

        Map<String, Map<String, Object>> moviesById = loadDocumentsById("movies", movieIds, true);
        Map<String, Map<String, Object>> theatresById = loadDocumentsById("theatres", theatreIds, true);
        Map<String, Map<String, Object>> screensById = loadDocumentsById("screens", screenIds, true);

        return new EnrichmentContext(
                seatStatusesByCode,
                customersById,
                showtimesById,
                moviesById,
                theatresById,
                screensById
        );
    }

    private Map<Integer, Map<String, Object>> loadSeatStatusesByCode(Set<Integer> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyMap();
        }

        Query query = Query.query(Criteria.where("code").in(codes)
                .and("isActive").is(true)
                .and("deletedAt").is(null));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) (List<?>)
                mongoTemplate.find(query, Map.class, "seat_statuses");

        Map<Integer, Map<String, Object>> byCode = new HashMap<>();
        for (Map<String, Object> doc : docs) {
            Integer code = toInt(doc.get("code"));
            if (code != null) {
                byCode.put(code, doc);
            }
        }
        return byCode;
    }

    private Map<String, Map<String, Object>> loadDocumentsById(String collectionName, Set<String> ids, boolean activeOnly) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> idCandidates = toIdCandidates(ids);
        if (idCandidates.isEmpty()) {
            return Collections.emptyMap();
        }

        Criteria criteria = Criteria.where("_id").in(idCandidates).and("deletedAt").is(null);
        if (activeOnly) {
            criteria = criteria.and("isActive").is(true);
        }

        Query query = Query.query(criteria);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) (List<?>)
                mongoTemplate.find(query, Map.class, collectionName);

        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> doc : docs) {
            Object idObj = doc.get("_id");
            if (idObj != null) {
                byId.put(idObj.toString(), doc);
            }
        }
        return byId;
    }

    private List<Object> toIdCandidates(Set<String> ids) {
        List<Object> candidates = new ArrayList<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            candidates.add(id);
            if (ObjectId.isValid(id)) {
                candidates.add(new ObjectId(id));
            }
        }
        return candidates;
    }

    private void addId(Set<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value);
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String str(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private record EnrichmentContext(
            Map<Integer, Map<String, Object>> seatStatusesByCode,
            Map<String, Map<String, Object>> customersById,
            Map<String, Map<String, Object>> showtimesById,
            Map<String, Map<String, Object>> moviesById,
            Map<String, Map<String, Object>> theatresById,
            Map<String, Map<String, Object>> screensById
    ) {
        private static EnrichmentContext empty() {
            return new EnrichmentContext(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }
    }
}
