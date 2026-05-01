package com.awal.cineq.publicapi.showtime.service.impl;

import com.awal.cineq.booking.model.Booking;
import com.awal.cineq.booking.model.BookingDetail;
import com.awal.cineq.booking.repository.BookingRepository;
import com.awal.cineq.booking.dto.BookingDetailResponse;
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
import com.awal.cineq.publicapi.showtime.dto.ScreenInfo;
import com.awal.cineq.publicapi.showtime.dto.ShowtimeListDTO;
import com.awal.cineq.publicapi.showtime.dto.TheaterInfo;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicRequest;
import com.awal.cineq.publicapi.showtime.dto.BookingPublicResponse;
import com.awal.cineq.publicapi.showtime.dto.SuggestSeatsRequest;
import com.awal.cineq.publicapi.showtime.dto.SuggestSeatsResponse;
import com.awal.cineq.publicapi.showtime.dto.SeatSuggestion;
import com.awal.cineq.publicapi.showtime.dto.SeatPreference;
import com.awal.cineq.publicapi.showtime.service.PublicShowtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.bson.types.ObjectId;
import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicShowtimeServiceImpl implements PublicShowtimeService {

    private final FrontendShowtimeService frontendShowtimeService;
    private final FrontendShowtimeRepository showtimeRepository;
    private final MongoTemplate mongoTemplate;
    private final FrontendScreenRepository screenRepository;
    private final FrontendTheatreRepository theatreRepository;
    private final BookingRepository bookingRepository;

    @Override
    public PaginationResponse<ShowtimeListDTO> getAllShowtimes(ShowtimePageRequest pageRequest) {
        log.info("STARTED getAllShowtimes");
        PaginationResponse<ShowtimeDTO> result = frontendShowtimeService.getAllShowtimes(pageRequest);
        PaginationResponse<ShowtimeListDTO> convertedResult = convertPaginationResponseOld(result);
        log.info("END getAllShowtimes");
        return convertedResult;
    }

    /**
     * Convert PaginationResponse<ShowtimeDTO> to PaginationResponse<ShowtimeListDTO>
     * Used for methods that still depend on FrontendShowtimeService
     */
    private PaginationResponse<ShowtimeListDTO> convertPaginationResponseOld(PaginationResponse<ShowtimeDTO> showtimeDTOResponse) {
        List<ShowtimeListDTO> convertedData = showtimeDTOResponse.getData().stream()
                .map(showtimeDTO -> convertToShowtimeListDTOOld(showtimeDTO, showtimeDTO.getTheatreId(), showtimeDTO.getScreenId()))
                .collect(Collectors.toList());

        PaginationResponse<ShowtimeListDTO> response = new PaginationResponse<>();
        response.setData(convertedData);
        response.setPage(showtimeDTOResponse.getPage());
        response.setSize(showtimeDTOResponse.getSize());
        response.setTotalElements(showtimeDTOResponse.getTotalElements());
        response.setTotalPages(showtimeDTOResponse.getTotalPages());
        response.setHasNext(showtimeDTOResponse.isHasNext());
        response.setHasPrevious(showtimeDTOResponse.isHasPrevious());
        return response;
    }

    /**
     * Convert ShowtimeDTO to ShowtimeListDTO with theatre and screen details
     * Used for methods that still depend on FrontendShowtimeService
     */
    private ShowtimeListDTO convertToShowtimeListDTOOld(ShowtimeDTO showtimeDTO, String theatreId, String screenId) {
        // Fetch theatre data
        TheaterInfo theaterInfo = null;
        if (theatreId != null) {
            Map<String, Object> theatreMap = theatreRepository.findByIdAndActive(theatreId);
            if (theatreMap != null) {
                theaterInfo = TheaterInfo.builder()
                        .id(theatreId)
                        .name((String) theatreMap.get("name"))
                        .build();
            }
        }

        // Fetch screen data
        ScreenInfo screenInfo = null;
        if (screenId != null) {
            Map<String, Object> screenMap = screenRepository.findByIdAndActive(screenId);
            if (screenMap != null) {
                screenInfo = ScreenInfo.builder()
                        .id(screenId)
                        .title((String) screenMap.get("screenName"))
                        .build();
            }
        }

        return ShowtimeListDTO.builder()
                .id(showtimeDTO.getId())
                .showDate(showtimeDTO.getShowDate())
                .showTime(showtimeDTO.getShowTime())
                .theater(theaterInfo)
                .screen(screenInfo)
                .build();
    }

    @Override
    public ApiResponse<Object> getShowtimeById(String id) {
        log.info("STARTED getShowtimeById: id={}", id);
        
        // Use MongoTemplate to query with ObjectId conversion
        Query query = new Query(Criteria.where("_id").is(new org.bson.types.ObjectId(id))
                .and("isActive").is(true)
                .and("deletedAt").is(null));
        
        Map<String, Object> showtimeMap = mongoTemplate.findOne(query, Map.class, "showtimes");
        if (showtimeMap == null) {
            log.error("ERROR getShowtimeById: showtime not found for id={}", id);
            throw new ResourceNotFoundException("Showtime not found: " + id);
        }
        
        // Remove formManagerId and formStepId from response
        showtimeMap.remove("formManagerId");
        showtimeMap.remove("formStepId");
        
        // Convert ObjectId to String if needed
        if (showtimeMap.get("_id") != null) {
            showtimeMap.put("id", showtimeMap.get("_id").toString());
            showtimeMap.remove("_id");
        }
        
        log.info("END getShowtimeById: id={}, fields={}", id, showtimeMap.keySet().size());
        
        // Return the map as-is from database (all fields except formManagerId and formStepId)
        return ApiResponse.success("Showtime fetched successfully", showtimeMap);
    }

    @Override
    public PaginationResponse<ShowtimeListDTO> getShowtimesByMovieId(String movieId, int page, int size) {
        log.info("STARTED getShowtimesByMovieId: movieId={}", movieId);
        
        // Get today's date in YYYY-MM-DD format
        String todayAsString = LocalDate.now().toString(); // e.g., "2026-04-29"

        // Use MongoTemplate to query with camelCase field names (as stored in MongoDB by form manager)
        Query query = new Query(Criteria.where("movieId").is(movieId)
                .and("showDate").gte(todayAsString) // String comparison works here!
                .and("isActive").is(true)
                .and("deletedAt").is(null));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> showtimes = 
            (List<Map<String, Object>>) (List<?>) mongoTemplate.find(query, Map.class, "showtimes");
        log.info("Found {} showtimes for movieId={}", showtimes.size(), movieId);
        
        // Extract unique theatreIds and screenIds for batch loading
        Set<String> theatreIds = new HashSet<>();
        Set<String> screenIds = new HashSet<>();
        for (Map<String, Object> showtime : showtimes) {
            String theatreId = (String) showtime.get("theatreId");
            String screenId = (String) showtime.get("screenId");
            if (theatreId != null) theatreIds.add(theatreId);
            if (screenId != null) screenIds.add(screenId);
        }
        
        // Batch load all theatres in one query
        Map<String, Map<String, Object>> theatreMap = new HashMap<>();
        if (!theatreIds.isEmpty()) {
            Query theatreQuery = new Query(Criteria.where("_id").in(
                    theatreIds.stream().map(ObjectId::new).collect(Collectors.toList()))
                    .and("isActive").is(true)
                    .and("deletedAt").is(null));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> theatres = 
                (List<Map<String, Object>>) (List<?>) mongoTemplate.find(theatreQuery, Map.class, "theatres");
            for (Map<String, Object> theatre : theatres) {
                String id = theatre.get("_id").toString();
                theatreMap.put(id, theatre);
            }
        }
        
        // Batch load all screens in one query
        Map<String, Map<String, Object>> screenMap = new HashMap<>();
        if (!screenIds.isEmpty()) {
            Query screenQuery = new Query(Criteria.where("_id").in(
                    screenIds.stream().map(ObjectId::new).collect(Collectors.toList()))
                    .and("isActive").is(true)
                    .and("deletedAt").is(null));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> screens = 
                (List<Map<String, Object>>) (List<?>) mongoTemplate.find(screenQuery, Map.class, "screens");
            for (Map<String, Object> screen : screens) {
                String id = screen.get("_id").toString();
                screenMap.put(id, screen);
            }
        }
        
        // Convert to ShowtimeListDTO using enriched data
        List<ShowtimeListDTO> convertedData = showtimes.stream()
                .map(showtime -> convertMapToShowtimeListDTO(showtime, theatreMap, screenMap))
                .collect(Collectors.toList());
        
        // Build pagination response (all data in first page)
        PaginationResponse<ShowtimeListDTO> response = new PaginationResponse<>();
        response.setData(convertedData);
        response.setPage(0);
        response.setSize(convertedData.size());
        response.setTotalElements(convertedData.size());
        response.setTotalPages(1);
        response.setHasNext(false);
        response.setHasPrevious(false);
        
        log.info("END getShowtimesByMovieId: movieId={}, totalShowtimes={}", movieId, convertedData.size());
        return response;
    }
    
    /**
     * Convert raw MongoDB Map to ShowtimeListDTO with theatre and screen details (using pre-loaded maps)
     */
    private ShowtimeListDTO convertMapToShowtimeListDTO(Map<String, Object> showtimeMap, 
            Map<String, Map<String, Object>> theatreMap, Map<String, Map<String, Object>> screenMap) {
        String theatreId = (String) showtimeMap.get("theatreId");
        String screenId = (String) showtimeMap.get("screenId");
        String movieId = (String) showtimeMap.get("movieId");
        String showDate = (String) showtimeMap.get("showDate");
        String showTime = (String) showtimeMap.get("showTime");
        String statusCode = (String) showtimeMap.get("statusCode");
        String id = showtimeMap.get("_id") != null ? showtimeMap.get("_id").toString() : null;
        
        // Get theatre info from pre-loaded map
        TheaterInfo theaterInfo = null;
        if (theatreId != null && theatreMap.containsKey(theatreId)) {
            Map<String, Object> theatre = theatreMap.get(theatreId);
            theaterInfo = TheaterInfo.builder()
                    .id(theatreId)
                    .name((String) theatre.get("name"))
                    .build();
        } else if (theatreId != null) {
            // Fallback if not in map (shouldn't happen with batch load)
            theaterInfo = TheaterInfo.builder()
                    .id(theatreId)
                    .name(null)
                    .build();
        }
        
        // Get screen info from pre-loaded map
        ScreenInfo screenInfo = null;
        if (screenId != null && screenMap.containsKey(screenId)) {
            Map<String, Object> screen = screenMap.get(screenId);
            screenInfo = ScreenInfo.builder()
                    .id(screenId)
                    .title((String) screen.get("screenName"))
                    .build();
        } else if (screenId != null) {
            // Fallback if not in map (shouldn't happen with batch load)
            screenInfo = ScreenInfo.builder()
                    .id(screenId)
                    .title(null)
                    .build();
        }
        
        return ShowtimeListDTO.builder()
                .id(id)
                .movieId(movieId)
                .showDate(showDate)
                .showTime(showTime)
                .theater(theaterInfo)
                .screen(screenInfo)
                .statusCode(statusCode)
                .build();
    }

    @Override
    public PaginationResponse<ShowtimeListDTO> getShowtimesByTheatreId(String theatreId, int page, int size) {
        log.info("STARTED getShowtimesByTheatreId: theatreId={}", theatreId);
        PaginationResponse<ShowtimeDTO> result = frontendShowtimeService.getShowtimesByTheatreId(theatreId, page, size);
        PaginationResponse<ShowtimeListDTO> convertedResult = convertPaginationResponseOld(result);
        log.info("END getShowtimesByTheatreId: theatreId={}", theatreId);
        return convertedResult;
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
        // Active bookings: Reserved(3) or Booked(2) — deletedAt=null enforced by repository query
        Set<String> takenSeats = bookings.stream()
                .filter(b -> b.getSeatStatusCode() != null
                          && (b.getSeatStatusCode() == 3 || b.getSeatStatusCode() == 2))
                .filter(b -> b.getBookingDetails() != null)
                .flatMap(b -> b.getBookingDetails().stream())
                .map(BookingDetail::getSeatName)
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

    // ─────────────────────────────────────────────────────────────
    // getPublicBookingsByShowtime
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<BookingPublicResponse> getPublicBookingsByShowtime(BookingPublicRequest request) {
        log.info("getPublicBookingsByShowtime — showtimeId={}", request.getShowtimeId());

        // Build query for bookings by showtimeId with paymentStatus COMPLETED or INITIATED
        Criteria criteria = new Criteria()
                .and("showtimeId").is(request.getShowtimeId())
                .and("paymentStatus").in("COMPLETED", "INITIATED")
                .and("deletedAt").is(null);

        Query query = new Query(criteria);
        List<Booking> bookings = mongoTemplate.find(query, Booking.class);

        // Map to minimal public response
        return bookings.stream()
                .map(booking -> BookingPublicResponse.builder()
                        .paymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : null)
                        .bookingDetails(mapBookingDetails(booking))
                        .createdAt(booking.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Map booking details from Booking model to BookingDetailResponse DTOs
     */
    private List<BookingDetailResponse> mapBookingDetails(Booking booking) {
        if (booking.getBookingDetails() == null) {
            return null;
        }
        return booking.getBookingDetails().stream()
                .map(detail -> BookingDetailResponse.builder()
                        .seatName(detail.getSeatName())
                        .row(detail.getRow())
                        .col(detail.getCol())
                        .seatCode(detail.getSeatCode())
                        .seatPrice(detail.getSeatPrice() != null ? detail.getSeatPrice().doubleValue() : null)
                        .seatStatusCode(detail.getSeatStatusCode())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // suggestSeats — Sliding Window + Greedy Scoring Algorithm
    // ─────────────────────────────────────────────────────────────

    @Override
    public ApiResponse<SuggestSeatsResponse> suggestSeats(SuggestSeatsRequest request) {
        log.info("STARTED suggestSeats: showtimeId={}, seats={}, preference={}",
                request.getShowtimeId(), request.getSeats(),
                request.getSeatPreference() != null ? request.getSeatPreference() : SeatPreference.MIDDLE);

        try {
            // 5a: Fetch Showtime Document
            Query showtimeQuery = new Query(Criteria.where("_id").is(new ObjectId(request.getShowtimeId()))
                    .and("isActive").is(true)
                    .and("deletedAt").is(null));
            Map<String, Object> showtime = mongoTemplate.findOne(showtimeQuery, Map.class, "showtimes");
            
            if (showtime == null) {
                log.error("ERROR suggestSeats: Showtime not found for id={}", request.getShowtimeId());
                throw new ResourceNotFoundException("Showtime not found: " + request.getShowtimeId());
            }

            // Extract seatLayout from showtime
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> seatLayout = 
                (List<Map<String, Object>>) showtime.get("seatLayout");
            
            if (seatLayout == null || seatLayout.isEmpty()) {
                log.warn("WARN suggestSeats: No seat layout found for showtime={}", request.getShowtimeId());
                return ApiResponse.success("No contiguous seats available for the requested count",
                        SuggestSeatsResponse.builder()
                                .showtimeId(request.getShowtimeId())
                                .requestedSeats(request.getSeats())
                                .suggestions(new ArrayList<>())
                                .build());
            }

            // 5b: Fetch Taken Seats
            Criteria criteria = new Criteria()
                    .and("showtimeId").is(request.getShowtimeId())
                    .and("paymentStatus").in("COMPLETED", "INITIATED")
                    .and("deletedAt").is(null);
            Query bookingQuery = new Query(criteria);
            List<Booking> bookings = mongoTemplate.find(bookingQuery, Booking.class);

            Set<String> takenSeats = bookings.stream()
                    .filter(b -> b.getBookingDetails() != null)
                    .flatMap(b -> b.getBookingDetails().stream())
                    .map(BookingDetail::getSeatName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            log.info("Found {} taken seats for showtime={}", takenSeats.size(), request.getShowtimeId());

            // 5c: Build Row-Grouped Data Structure
            Map<String, List<Map<String, Object>>> rowGroups = new TreeMap<>();
            int totalCols = 0;

            for (Map<String, Object> seat : seatLayout) {
                String row = (String) seat.get("row");
                Object colObj = seat.get("col");
                Integer col = colObj instanceof Integer ? (Integer) colObj : 
                             colObj instanceof Number ? ((Number) colObj).intValue() : null;

                if (row != null && col != null) {
                    rowGroups.computeIfAbsent(row, k -> new ArrayList<>()).add(seat);
                    if (col > totalCols) {
                        totalCols = col;
                    }
                }
            }

            // Sort each row by column
            for (List<Map<String, Object>> rowSeats : rowGroups.values()) {
                rowSeats.sort((a, b) -> {
                    Integer colA = getIntValue(a.get("col"));
                    Integer colB = getIntValue(b.get("col"));
                    return Integer.compare(colA != null ? colA : 0, colB != null ? colB : 0);
                });
            }

            int totalRows = rowGroups.size();
            List<String> rowList = new ArrayList<>(rowGroups.keySet());

            // 5d: Sliding Window + Greedy Scoring Algorithm
            List<WindowCandidate> candidates = new ArrayList<>();
            int requestedSeats = request.getSeats();

            for (int rowIdx = 0; rowIdx < rowList.size(); rowIdx++) {
                String currentRow = rowList.get(rowIdx);
                List<Map<String, Object>> rowSeats = rowGroups.get(currentRow);

                // Slide window across the row
                for (int startIdx = 0; startIdx <= rowSeats.size() - requestedSeats; startIdx++) {
                    List<Map<String, Object>> windowSeats = rowSeats.subList(startIdx, startIdx + requestedSeats);

                    // Check if all seats in window are free
                    boolean allFree = windowSeats.stream()
                            .map(s -> (String) s.get("seatName"))
                            .allMatch(seatName -> !takenSeats.contains(seatName));

                    if (allFree) {
                        // Compute score
                        Integer startCol = getIntValue(windowSeats.get(0).get("col"));
                        Integer endCol = getIntValue(windowSeats.get(windowSeats.size() - 1).get("col"));

                        if (startCol != null && endCol != null) {
                            // Calculate row score based on seat preference
                            double rowScore = calculateRowScore(rowIdx, totalRows, request.getSeatPreference());
                            double windowCenterCol = startCol + requestedSeats / 2.0;
                            double colScore = 100 - Math.abs(windowCenterCol - (totalCols / 2.0)) * 15;
                            double score = rowScore + colScore;

                            // Calculate total price
                            double totalPrice = windowSeats.stream()
                                    .map(s -> s.get("price"))
                                    .mapToDouble(p -> p instanceof Number ? ((Number) p).doubleValue() : 0.0)
                                    .sum();

                            candidates.add(new WindowCandidate(
                                    rowIdx, currentRow, startCol, endCol, score, windowSeats, totalPrice
                            ));
                        }
                    }
                }
            }

            // 5e: Sort and Pick Top 3
            candidates.sort((a, b) -> Double.compare(b.score, a.score));
            
            List<SeatSuggestion> suggestions = new ArrayList<>();
            for (int i = 0; i < Math.min(3, candidates.size()); i++) {
                WindowCandidate candidate = candidates.get(i);
                List<SeatInfo> seatInfos = candidate.windowSeats.stream()
                        .map(this::mapSeatToSeatInfo)
                        .collect(Collectors.toList());

                suggestions.add(SeatSuggestion.builder()
                        .rank(i + 1)
                        .row(candidate.row)
                        .startCol(candidate.startCol)
                        .endCol(candidate.endCol)
                        .score(candidate.score)
                        .seats(seatInfos)
                        .totalPrice(candidate.totalPrice)
                        .build());
            }

            // 5f: Build and Return Response
            SuggestSeatsResponse response = SuggestSeatsResponse.builder()
                    .showtimeId(request.getShowtimeId())
                    .requestedSeats(requestedSeats)
                    .suggestions(suggestions)
                    .build();

            String message = suggestions.isEmpty() 
                    ? "No contiguous seats available for the requested count"
                    : "Seats suggested successfully";
            
            log.info("END suggestSeats: showtimeId={}, suggestions={}, message={}", 
                    request.getShowtimeId(), suggestions.size(), message);
            
            return ApiResponse.success(message, response);

        } catch (ResourceNotFoundException e) {
            log.error("RESOURCE NOT FOUND in suggestSeats: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("ERROR in suggestSeats: ", e);
            throw e;
        }
    }

    /**
     * Helper: Convert integer from Map with null safety
     */
    private Integer getIntValue(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        return null;
    }

    /**
     * Helper: Calculate row score based on customer's seat preference
     * FRONT: Favor rows near the front (index 0-2)
     * BACK: Favor rows near the back (index 7-9)
     * MIDDLE: Favor rows in the center (default behavior)
     */
    private double calculateRowScore(int rowIdx, int totalRows, SeatPreference preference) {
        if (preference == null) {
            preference = SeatPreference.MIDDLE;  // default
        }

        switch (preference) {
            case FRONT:
                // Favor front rows (smaller index is better)
                // Row 0 (A) gets score ~100, Row 1 (B) gets ~85, decreasing towards back
                return 100 - (rowIdx * 15.0 / totalRows);

            case BACK:
                // Favor back rows (larger index is better)
                // Last row gets score ~100, second-last gets ~85, decreasing towards front
                int distFromBack = totalRows - 1 - rowIdx;
                return 100 - (distFromBack * 15.0 / totalRows);

            case MIDDLE:
            default:
                // Favor middle rows (original behavior)
                // Center rows get highest score
                double midRow = (totalRows - 1) / 2.0;
                return 100 - Math.abs(rowIdx - midRow) * 20;
        }
    }

    /**
     * Helper: Map seat Map to SeatInfo DTO
     */
    private SeatInfo mapSeatToSeatInfo(Map<String, Object> seatMap) {
        return SeatInfo.builder()
                .seatNumber((String) seatMap.get("seatName"))
                .row((String) seatMap.get("row"))
                .column(getIntValue(seatMap.get("col")))
                .seatType((String) seatMap.get("code"))
                .price(seatMap.get("price") instanceof Number 
                        ? ((Number) seatMap.get("price")).doubleValue() 
                        : 0.0)
                .build();
    }

    /**
     * Inner class: Represents a candidate window for seat suggestion
     */
    private static class WindowCandidate {
        final int rowIdx;
        final String row;
        final int startCol;
        final int endCol;
        final double score;
        final List<Map<String, Object>> windowSeats;
        final double totalPrice;

        WindowCandidate(int rowIdx, String row, int startCol, int endCol, double score,
                       List<Map<String, Object>> windowSeats, double totalPrice) {
            this.rowIdx = rowIdx;
            this.row = row;
            this.startCol = startCol;
            this.endCol = endCol;
            this.score = score;
            this.windowSeats = windowSeats;
            this.totalPrice = totalPrice;
        }
    }
}

