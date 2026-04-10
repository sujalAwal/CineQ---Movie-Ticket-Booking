package com.awal.cineq.booking.service.impl;

import com.awal.cineq.booking.dto.BookingDetailResponse;
import com.awal.cineq.booking.dto.BookingResponse;
import com.awal.cineq.booking.dto.ConfirmBookingRequest;
import com.awal.cineq.booking.dto.CreateBookingRequest;
import com.awal.cineq.booking.dto.SeatRequest;
import com.awal.cineq.booking.model.Booking;
import com.awal.cineq.booking.model.BookingDetail;
import com.awal.cineq.booking.repository.BookingRepository;
import com.awal.cineq.booking.service.BookingService;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.movies.repository.FrontendMovieRepository;
import com.awal.cineq.frontend.screens.repository.FrontendScreenRepository;
import com.awal.cineq.frontend.showtimes.repository.FrontendShowtimeRepository;
import com.awal.cineq.frontend.theatres.repository.FrontendTheatreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final FrontendShowtimeRepository showtimeRepository;
    private final FrontendMovieRepository movieRepository;
    private final FrontendTheatreRepository theatreRepository;
    private final FrontendScreenRepository screenRepository;

    // ─────────────────────────────────────────────────────────────
    // createBooking
    // ─────────────────────────────────────────────────────────────

    @Override
    public BookingResponse createBooking(String customerId, CreateBookingRequest request) {
        log.info("createBooking STARTED — customerId={}, showtimeId={}", customerId, request.getShowtimeId());

        // 1. Fetch showtime
        Map<String, Object> showtime = showtimeRepository.findByIdAndActive(request.getShowtimeId());
        if (showtime == null) {
            throw new ResourceNotFoundException("Showtime not found with id: " + request.getShowtimeId());
        }

        // 2. Check showtime availability
        Object statusCode = showtime.get("statusCode");
        if ("HF".equals(statusCode) || "X".equals(statusCode)) {
            throw new IllegalArgumentException("Showtime is not available for booking");
        }

        // 3. Resolve base price
        double basePrice = ((Number) showtime.get("basePrice")).doubleValue();

        // 4. Collect already-booked seats for this showtime
        List<Booking> existingBookings = bookingRepository.findByShowtimeId(request.getShowtimeId());
        Set<String> takenSeats = existingBookings.stream()
                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.PENDING
                        || b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .filter(b -> b.getBookingDetails() != null)
                .flatMap(b -> b.getBookingDetails().stream())
                .map(BookingDetail::getSeatNumber)
                .collect(Collectors.toCollection(HashSet::new));

        // 5 & 6 & 7. Validate seats, compute prices, build BookingDetail list
        List<BookingDetail> bookingDetails = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SeatRequest seatRequest : request.getSeats()) {
            String seatNumber = seatRequest.getSeatNumber();
            if (takenSeats.contains(seatNumber)) {
                throw new IllegalArgumentException("Seat " + seatNumber + " is already booked");
            }

            double multiplier = resolveMultiplier(seatRequest.getSeatType());
            BigDecimal seatPrice = BigDecimal.valueOf(basePrice).multiply(BigDecimal.valueOf(multiplier));

            BookingDetail detail = new BookingDetail();
            detail.setSeatNumber(seatNumber);
            detail.setSeatType(seatRequest.getSeatType());
            detail.setSeatPrice(seatPrice);
            detail.setCreatedAt(LocalDateTime.now());
            bookingDetails.add(detail);

            totalAmount = totalAmount.add(seatPrice);
        }

        // 8. Generate reference
        String bookingReference = "BK" + System.currentTimeMillis();

        // 9. Build and persist Booking
        Booking booking = new Booking();
        booking.setBookingReference(bookingReference);
        booking.setShowtimeId(request.getShowtimeId());
        booking.setCustomerId(customerId);
        booking.setBookingDate(LocalDateTime.now());
        booking.setNumberOfSeats(request.getSeats().size());
        booking.setTotalAmount(totalAmount);
        booking.setBookingStatus(Booking.BookingStatus.PENDING);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
        booking.setPaymentMethod(request.getPaymentMethod());
        booking.setBookingDetails(bookingDetails);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);
        log.info("createBooking END — bookingReference={}", bookingReference);

        // 10. Build enriched response
        return buildBookingResponse(savedBooking, showtime);
    }

    // ─────────────────────────────────────────────────────────────
    // getMyBookings
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<BookingResponse> getMyBookings(String customerId) {
        log.info("getMyBookings STARTED — customerId={}", customerId);

        List<Booking> bookings = bookingRepository.findByCustomerId(customerId);

        List<BookingResponse> responses = bookings.stream()
                .sorted(Comparator.comparing(
                        b -> b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .map(booking -> {
                    Map<String, Object> showtime = null;
                    if (booking.getShowtimeId() != null) {
                        showtime = showtimeRepository.findByIdAndActive(booking.getShowtimeId());
                    }
                    return buildBookingResponse(booking, showtime);
                })
                .collect(Collectors.toList());

        log.info("getMyBookings END — customerId={}, count={}", customerId, responses.size());
        return responses;
    }

    // ─────────────────────────────────────────────────────────────
    // getBookingByReference
    // ─────────────────────────────────────────────────────────────

    @Override
    public BookingResponse getBookingByReference(String bookingReference, String customerId) {
        log.info("getBookingByReference STARTED — reference={}, customerId={}", bookingReference, customerId);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!customerId.equals(booking.getCustomerId())) {
            throw new ResourceNotFoundException("Booking not found");
        }

        Map<String, Object> showtime = null;
        if (booking.getShowtimeId() != null) {
            showtime = showtimeRepository.findByIdAndActive(booking.getShowtimeId());
        }

        BookingResponse response = buildBookingResponse(booking, showtime);
        log.info("getBookingByReference END — reference={}", bookingReference);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // confirmBooking
    // ─────────────────────────────────────────────────────────────

    @Override
    public BookingResponse confirmBooking(String bookingReference, String customerId, ConfirmBookingRequest request) {
        log.info("confirmBooking STARTED — reference={}, customerId={}", bookingReference, customerId);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!customerId.equals(booking.getCustomerId())) {
            throw new ResourceNotFoundException("Booking not found");
        }

        if (booking.getBookingStatus() != Booking.BookingStatus.PENDING) {
            throw new IllegalStateException("Only PENDING bookings can be confirmed");
        }

        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.COMPLETED);

        String paymentRef = (request != null && request.getPaymentReference() != null)
                ? request.getPaymentReference()
                : "PAY-" + System.currentTimeMillis();
        booking.setPaymentReference(paymentRef);

        if (request != null && request.getPaymentMethod() != null) {
            booking.setPaymentMethod(request.getPaymentMethod());
        }

        booking.setUpdatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        Map<String, Object> showtime = null;
        if (savedBooking.getShowtimeId() != null) {
            showtime = showtimeRepository.findByIdAndActive(savedBooking.getShowtimeId());
        }

        BookingResponse response = buildBookingResponse(savedBooking, showtime);
        log.info("confirmBooking END — reference={}", bookingReference);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // cancelBooking
    // ─────────────────────────────────────────────────────────────

    @Override
    public BookingResponse cancelBooking(String bookingReference, String customerId) {
        log.info("cancelBooking STARTED — reference={}, customerId={}", bookingReference, customerId);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!customerId.equals(booking.getCustomerId())) {
            throw new ResourceNotFoundException("Booking not found");
        }

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED
                || booking.getBookingStatus() == Booking.BookingStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Booking is already " + booking.getBookingStatus().name().toLowerCase());
        }

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        Map<String, Object> showtime = null;
        if (savedBooking.getShowtimeId() != null) {
            showtime = showtimeRepository.findByIdAndActive(savedBooking.getShowtimeId());
        }

        BookingResponse response = buildBookingResponse(savedBooking, showtime);
        log.info("cancelBooking END — reference={}", bookingReference);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private double resolveMultiplier(String seatType) {
        if (seatType == null) return 1.0;
        return switch (seatType.toUpperCase()) {
            case "PREMIUM" -> 1.5;
            case "VIP"     -> 2.0;
            default        -> 1.0; // STANDARD
        };
    }

    private BookingResponse buildBookingResponse(Booking booking, Map<String, Object> showtime) {
        BookingResponse.BookingResponseBuilder builder = BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .showtimeId(booking.getShowtimeId())
                .customerId(booking.getCustomerId())
                .bookingDate(booking.getBookingDate() != null ? booking.getBookingDate().toString() : null)
                .numberOfSeats(booking.getNumberOfSeats())
                .totalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount().doubleValue() : null)
                .bookingStatus(booking.getBookingStatus() != null ? booking.getBookingStatus().name() : null)
                .paymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : null)
                .paymentMethod(booking.getPaymentMethod())
                .paymentReference(booking.getPaymentReference());

        // Map booking details
        if (booking.getBookingDetails() != null) {
            List<BookingDetailResponse> detailResponses = booking.getBookingDetails().stream()
                    .sorted(Comparator.comparing(
                            d -> d.getSeatNumber() != null ? d.getSeatNumber() : "",
                            Comparator.naturalOrder()))
                    .map(d -> new BookingDetailResponse(
                            d.getSeatNumber(),
                            d.getSeatType(),
                            d.getSeatPrice() != null ? d.getSeatPrice().doubleValue() : null))
                    .collect(Collectors.toList());
            builder.bookingDetails(detailResponses);
        }

        // Enrich with showtime data
        if (showtime != null) {
            String movieId   = getStringValue(showtime, "movieId");
            String theatreId = getStringValue(showtime, "theatreId");
            String screenId  = getStringValue(showtime, "screenId");

            builder.movieId(movieId)
                    .theatreId(theatreId)
                    .screenId(screenId)
                    .showDate(getStringValue(showtime, "showDate"))
                    .showTime(getStringValue(showtime, "showTime"))
                    .language(getStringValue(showtime, "language"))
                    .format(getStringValue(showtime, "format"));

            // Enrich movie
            if (movieId != null) {
                try {
                    Map<String, Object> movie = movieRepository.findByIdAndActive(movieId);
                    if (movie != null) {
                        builder.movieTitle(getStringValue(movie, "title"))
                               .moviePoster(getStringValue(movie, "poster"));
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch movie data for movieId={}: {}", movieId, e.getMessage());
                }
            }

            // Enrich theatre
            if (theatreId != null) {
                try {
                    Map<String, Object> theatre = theatreRepository.findByIdAndActive(theatreId);
                    if (theatre != null) {
                        builder.theatreName(getStringValue(theatre, "name"));
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch theatre data for theatreId={}: {}", theatreId, e.getMessage());
                }
            }

            // Enrich screen
            if (screenId != null) {
                try {
                    Map<String, Object> screen = screenRepository.findByIdAndActive(screenId);
                    if (screen != null) {
                        builder.screenName(getStringValue(screen, "screenName"));
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch screen data for screenId={}: {}", screenId, e.getMessage());
                }
            }
        }

        return builder.build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
