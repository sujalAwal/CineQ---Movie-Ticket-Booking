package com.awal.cineq.publicapi.showtime.service.impl;

import com.awal.cineq.booking.model.Booking;
import com.awal.cineq.booking.model.BookingDetail;
import com.awal.cineq.booking.repository.BookingRepository;
import com.awal.cineq.dto.ApiResponse;
import com.awal.cineq.dto.PaginationResponse;
import com.awal.cineq.exception.ResourceNotFoundException;
import com.awal.cineq.frontend.screens.repository.FrontendScreenRepository;
import com.awal.cineq.frontend.showtimes.dto.ShowtimeDTO;
import com.awal.cineq.frontend.showtimes.dto.request.ShowtimePageRequest;
import com.awal.cineq.frontend.showtimes.repository.FrontendShowtimeRepository;
import com.awal.cineq.frontend.showtimes.service.FrontendShowtimeService;
import com.awal.cineq.frontend.theatres.repository.FrontendTheatreRepository;
import com.awal.cineq.publicapi.showtime.dto.SeatAvailabilityResponse;
import com.awal.cineq.publicapi.showtime.dto.SeatInfo;
import com.awal.cineq.publicapi.showtime.service.PublicShowtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicShowtimeServiceImpl implements PublicShowtimeService {

    private final FrontendShowtimeService frontendShowtimeService;
    private final FrontendShowtimeRepository showtimeRepository;
    private final FrontendScreenRepository screenRepository;
    private final FrontendTheatreRepository theatreRepository;
    private final BookingRepository bookingRepository;

    @Override
    public PaginationResponse<ShowtimeDTO> getAllShowtimes(ShowtimePageRequest pageRequest) {
        log.info("STARTED getAllShowtimes");
        PaginationResponse<ShowtimeDTO> result = frontendShowtimeService.getAllShowtimes(pageRequest);
        log.info("END getAllShowtimes");
        return result;
    }

    @Override
    public ApiResponse<ShowtimeDTO> getShowtimeById(String id) {
        log.info("STARTED getShowtimeById: id={}", id);
        ShowtimeDTO showtime = frontendShowtimeService.getShowtimeById(id);
        if (showtime == null) {
            log.error("ERROR getShowtimeById: showtime not found for id={}", id);
            throw new ResourceNotFoundException("Showtime not found: " + id);
        }
        log.info("END getShowtimeById: id={}", id);
        return ApiResponse.success("Showtime fetched successfully", showtime);
    }

    @Override
    public PaginationResponse<ShowtimeDTO> getShowtimesByMovieId(String movieId, int page, int size) {
        log.info("STARTED getShowtimesByMovieId: movieId={}", movieId);
        PaginationResponse<ShowtimeDTO> result = frontendShowtimeService.getShowtimesByMovieId(movieId, page, size);
        log.info("END getShowtimesByMovieId: movieId={}", movieId);
        return result;
    }

    @Override
    public PaginationResponse<ShowtimeDTO> getShowtimesByTheatreId(String theatreId, int page, int size) {
        log.info("STARTED getShowtimesByTheatreId: theatreId={}", theatreId);
        PaginationResponse<ShowtimeDTO> result = frontendShowtimeService.getShowtimesByTheatreId(theatreId, page, size);
        log.info("END getShowtimesByTheatreId: theatreId={}", theatreId);
        return result;
    }

    @Override
    public ApiResponse<SeatAvailabilityResponse> getSeatAvailability(String showtimeId) {
        log.info("STARTED getSeatAvailability: showtimeId={}", showtimeId);

        // 1. Get showtime raw map
        Map<String, Object> showtime = showtimeRepository.findByIdAndActive(showtimeId);
        if (showtime == null) {
            log.error("ERROR getSeatAvailability: showtime not found for id={}", showtimeId);
            throw new ResourceNotFoundException("Showtime not found: " + showtimeId);
        }

        String screenId   = (String) showtime.get("screenId");
        String theatreId  = (String) showtime.get("theatreId");
        String movieId    = (String) showtime.get("movieId");
        String showDate   = (String) showtime.get("showDate");
        String showTime   = (String) showtime.get("showTime");
        String language   = (String) showtime.get("language");
        String format     = (String) showtime.get("format");
        String statusCode = (String) showtime.get("statusCode");
        Double basePrice  = showtime.get("basePrice") instanceof Number
                ? ((Number) showtime.get("basePrice")).doubleValue()
                : 0.0;

        // 2. Get screen dimensions
        Map<String, Object> screen = screenRepository.findByIdAndActive(screenId);
        int numRows = 8;  // default
        int numCols = 10; // default
        String screenName = null;
        if (screen != null) {
            Object r = screen.get("rows");
            Object c = screen.get("columns");
            if (r instanceof Number) numRows = ((Number) r).intValue();
            if (c instanceof Number) numCols = ((Number) c).intValue();
            screenName = (String) screen.get("screenName");
        }

        // 3. Get theatre name
        String theatreName = null;
        Map<String, Object> theatre = theatreRepository.findByIdAndActive(theatreId);
        if (theatre != null) {
            theatreName = (String) theatre.get("name");
        }

        // 4. Get taken seats (PENDING + CONFIRMED bookings)
        List<Booking> bookings = bookingRepository.findByShowtimeId(showtimeId);
        Set<String> takenSeats = bookings.stream()
                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.PENDING
                          || b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .filter(b -> b.getBookingDetails() != null)
                .flatMap(b -> b.getBookingDetails().stream())
                .map(BookingDetail::getSeatNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 5. Generate seat grid
        // Seat type by row position: first 30% = STANDARD, next 40% = PREMIUM, last 30% = VIP
        // Price multiplier: STANDARD=1.0x, PREMIUM=1.5x, VIP=2.0x
        List<SeatInfo> seats = new ArrayList<>();
        for (int rowIdx = 0; rowIdx < numRows; rowIdx++) {
            String rowLetter = String.valueOf((char) ('A' + rowIdx));
            String seatType;
            double multiplier;
            double rowPercent = (double) rowIdx / numRows;
            if (rowPercent < 0.3) {
                seatType = "STANDARD";
                multiplier = 1.0;
            } else if (rowPercent < 0.7) {
                seatType = "PREMIUM";
                multiplier = 1.5;
            } else {
                seatType = "VIP";
                multiplier = 2.0;
            }
            double seatPrice = Math.round(basePrice * multiplier * 100.0) / 100.0;

            for (int col = 1; col <= numCols; col++) {
                String seatNumber = rowLetter + col;
                boolean available = !takenSeats.contains(seatNumber);
                seats.add(SeatInfo.builder()
                        .seatNumber(seatNumber)
                        .row(rowLetter)
                        .column(col)
                        .seatType(seatType)
                        .price(seatPrice)
                        .isAvailable(available)
                        .build());
            }
        }

        int totalSeats = numRows * numCols;
        long availableCount = seats.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsAvailable()))
                .count();

        SeatAvailabilityResponse response = SeatAvailabilityResponse.builder()
                .showtimeId(showtimeId)
                .movieId(movieId)
                .theatreId(theatreId)
                .theatreName(theatreName)
                .screenId(screenId)
                .screenName(screenName)
                .showDate(showDate)
                .showTime(showTime)
                .language(language)
                .format(format)
                .statusCode(statusCode)
                .basePrice(basePrice)
                .rows(numRows)
                .columns(numCols)
                .totalSeats(totalSeats)
                .availableSeats((int) availableCount)
                .seats(seats)
                .build();

        log.info("END getSeatAvailability: showtimeId={}, totalSeats={}, availableSeats={}",
                showtimeId, totalSeats, availableCount);
        return ApiResponse.success("Seat availability fetched successfully", response);
    }
}
