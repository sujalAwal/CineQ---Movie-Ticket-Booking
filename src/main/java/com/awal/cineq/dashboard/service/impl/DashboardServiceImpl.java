package com.awal.cineq.dashboard.service.impl;

import com.awal.cineq.dashboard.dto.DashboardStatsDTO;
import com.awal.cineq.dashboard.dto.KpiCardsDTO;
import com.awal.cineq.dashboard.dto.MovieShowcaseDTO;
import com.awal.cineq.dashboard.dto.PaymentMethodDTO;
import com.awal.cineq.dashboard.dto.RecentBookingDTO;
import com.awal.cineq.dashboard.dto.RevenueTrendItemDTO;
import com.awal.cineq.dashboard.dto.StatusDistributionDTO;
import com.awal.cineq.dashboard.dto.TopMovieDTO;
import com.awal.cineq.dashboard.dto.TopTheatreDTO;
import com.awal.cineq.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private static final String BOOKINGS_COLLECTION = "bookings";
    private static final String MOVIES_COLLECTION = "movies";
    private static final String SHOWTIMES_COLLECTION = "showtimes";
    private static final String THEATRES_COLLECTION = "theatres";
    private static final String CUSTOMERS_COLLECTION = "customers";
    private static final String PAYMENTS_COLLECTION = "payments";
    private static final Set<Integer> VALID_PERIODS = Set.of(7, 15, 30);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MongoTemplate mongoTemplate;

    @Override
    public DashboardStatsDTO getDashboardStats(int period) {
        int normalizedPeriod = VALID_PERIODS.contains(period) ? period : 7;

        CompletableFuture<KpiCardsDTO> kpiFuture = CompletableFuture.supplyAsync(this::computeKpiCards);
        CompletableFuture<List<RevenueTrendItemDTO>> revenueTrendFuture =
                CompletableFuture.supplyAsync(() -> computeRevenueTrend(normalizedPeriod));
        CompletableFuture<List<StatusDistributionDTO>> bookingsByStatusFuture =
                CompletableFuture.supplyAsync(this::computeBookingsByStatus);
        CompletableFuture<List<TopMovieDTO>> topMoviesFuture = CompletableFuture.supplyAsync(this::computeTopMovies);
        CompletableFuture<List<TopTheatreDTO>> topTheatresFuture =
                CompletableFuture.supplyAsync(this::computeTopTheatres);
        CompletableFuture<List<MovieShowcaseDTO>> nowShowingFuture =
                CompletableFuture.supplyAsync(this::computeNowShowingMovies);
        CompletableFuture<List<MovieShowcaseDTO>> comingSoonFuture =
                CompletableFuture.supplyAsync(this::computeComingSoonMovies);
        CompletableFuture<List<RecentBookingDTO>> recentBookingsFuture =
                CompletableFuture.supplyAsync(this::computeRecentBookings);
        CompletableFuture<List<PaymentMethodDTO>> paymentMethodFuture =
                CompletableFuture.supplyAsync(this::computePaymentMethodDistribution);

        try {
            CompletableFuture.allOf(
                    kpiFuture,
                    revenueTrendFuture,
                    bookingsByStatusFuture,
                    topMoviesFuture,
                    topTheatresFuture,
                    nowShowingFuture,
                    comingSoonFuture,
                    recentBookingsFuture,
                    paymentMethodFuture
            ).join();
        } catch (CompletionException ex) {
            log.error("Failed to compute dashboard stats", ex.getCause() != null ? ex.getCause() : ex);
            throw new IllegalStateException("Failed to compute dashboard stats", ex.getCause());
        }

        return DashboardStatsDTO.builder()
                .kpiCards(kpiFuture.join())
                .revenueTrend(revenueTrendFuture.join())
                .bookingsByStatus(bookingsByStatusFuture.join())
                .topMovies(topMoviesFuture.join())
                .topTheatres(topTheatresFuture.join())
                .nowShowingMovies(nowShowingFuture.join())
                .comingSoonMovies(comingSoonFuture.join())
                .recentBookings(recentBookingsFuture.join())
                .paymentMethodDistribution(paymentMethodFuture.join())
                .build();
    }

    private KpiCardsDTO computeKpiCards() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        long totalBookings = mongoTemplate.count(
                Query.query(Criteria.where("deletedAt").is(null)),
                BOOKINGS_COLLECTION
        );

        long todaysBookings = mongoTemplate.count(
                Query.query(Criteria.where("deletedAt").is(null)
                        .and("createdAt").gte(startOfToday).lt(startOfTomorrow)),
                BOOKINGS_COLLECTION
        );

        BigDecimal totalRevenue = sumAmount(
                BOOKINGS_COLLECTION,
                Criteria.where("deletedAt").is(null).and("paymentStatus").is("COMPLETED")
        );

        BigDecimal todaysRevenue = sumAmount(
                BOOKINGS_COLLECTION,
                Criteria.where("deletedAt").is(null)
                        .and("paymentStatus").is("COMPLETED")
                        .and("createdAt").gte(startOfToday).lt(startOfTomorrow)
        );

        long nowShowingMovies = mongoTemplate.count(
                Query.query(Criteria.where("deletedAt").is(null)
                        .and("isActive").is(true)
                        .and("status").in("now_showing", "NOW_SHOWING")),
                MOVIES_COLLECTION
        );

        long activeTheatres = mongoTemplate.count(
                Query.query(Criteria.where("deletedAt").is(null).and("isActive").is(true)),
                THEATRES_COLLECTION
        );

        return KpiCardsDTO.builder()
                .totalBookings(totalBookings)
                .todaysBookings(todaysBookings)
                .totalRevenue(totalRevenue)
                .todaysRevenue(todaysRevenue)
                .nowShowingMovies(nowShowingMovies)
                .activeTheatres(activeTheatres)
                .build();
    }

    private List<RevenueTrendItemDTO> computeRevenueTrend(int period) {
        LocalDateTime trendStart = LocalDate.now().minusDays(period - 1L).atStartOfDay();

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deletedAt").is(null)
                        .and("paymentStatus").is("COMPLETED")
                        .and("createdAt").gte(trendStart)),
                projectBookingDateAndAmount(),
                Aggregation.group("date")
                        .sum("totalAmount").as("revenue")
                        .count().as("bookings"),
                Aggregation.project("revenue", "bookings").and("_id").as("date"),
                Aggregation.sort(Sort.by(Sort.Direction.ASC, "date"))
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, BOOKINGS_COLLECTION, Document.class);
        List<RevenueTrendItemDTO> items = new ArrayList<>();

        for (Document result : results.getMappedResults()) {
            items.add(RevenueTrendItemDTO.builder()
                    .date(stringValue(result.get("date")))
                    .revenue(toBigDecimal(result.get("revenue")))
                    .bookings(toLong(result.get("bookings")))
                    .build());
        }

        return items;
    }

    private List<StatusDistributionDTO> computeBookingsByStatus() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deletedAt").is(null)),
                Aggregation.group("seatStatusCode").count().as("count")
        );

        Map<Integer, Long> countByCode = new java.util.HashMap<>();
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, BOOKINGS_COLLECTION, Document.class);
        for (Document result : results.getMappedResults()) {
            Integer code = toInteger(result.get("_id"));
            if (code != null) {
                countByCode.put(code, toLong(result.get("count")));
            }
        }

        List<StatusDistributionDTO> items = new ArrayList<>();
        items.add(new StatusDistributionDTO("Confirmed", countByCode.getOrDefault(2, 0L)));
        items.add(new StatusDistributionDTO("Pending", countByCode.getOrDefault(3, 0L)));
        items.add(new StatusDistributionDTO("Cancelled", countByCode.getOrDefault(1, 0L)));
        return items;
    }

    private List<TopMovieDTO> computeTopMovies() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deletedAt").is(null).and("paymentStatus").is("COMPLETED")),
                addFieldsTotalAmountDouble(),
                lookupByStringId("showtimeId", SHOWTIMES_COLLECTION, "showtime", true, false),
                unwind("showtime", false),
                lookupByStringId("showtime.movieId", MOVIES_COLLECTION, "movie", true, false),
                unwind("movie", false),
                Aggregation.group("showtime.movieId")
                        .count().as("totalBookings")
                        .sum("totalAmount").as("totalRevenue")
                        .first("movie.title").as("title")
                        .first("movie.poster").as("poster"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "totalRevenue")),
                Aggregation.limit(5),
                Aggregation.project("title", "poster", "totalBookings", "totalRevenue")
                        .and("_id").as("movieId")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, BOOKINGS_COLLECTION, Document.class);
        List<TopMovieDTO> items = new ArrayList<>();

        for (Document result : results.getMappedResults()) {
            items.add(TopMovieDTO.builder()
                    .movieId(stringValue(result.get("movieId")))
                    .title(stringValue(result.get("title")))
                    .poster(stringValue(result.get("poster")))
                    .totalBookings(toLong(result.get("totalBookings")))
                    .totalRevenue(toBigDecimal(result.get("totalRevenue")))
                    .build());
        }

        return items;
    }

    private List<TopTheatreDTO> computeTopTheatres() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deletedAt").is(null).and("paymentStatus").is("COMPLETED")),
                addFieldsTotalAmountDouble(),
                lookupByStringId("showtimeId", SHOWTIMES_COLLECTION, "showtime", true, false),
                unwind("showtime", false),
                lookupByStringId("showtime.theatreId", THEATRES_COLLECTION, "theatre", true, false),
                unwind("theatre", false),
                Aggregation.group("showtime.theatreId")
                        .count().as("totalBookings")
                        .sum("totalAmount").as("totalRevenue")
                        .first("theatre.name").as("name")
                        .first("theatre.district").as("district"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "totalRevenue")),
                Aggregation.limit(5),
                Aggregation.project("name", "district", "totalBookings", "totalRevenue")
                        .and("_id").as("theatreId")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, BOOKINGS_COLLECTION, Document.class);
        List<TopTheatreDTO> items = new ArrayList<>();

        for (Document result : results.getMappedResults()) {
            items.add(TopTheatreDTO.builder()
                    .theatreId(stringValue(result.get("theatreId")))
                    .name(stringValue(result.get("name")))
                    .district(stringValue(result.get("district")))
                    .totalBookings(toLong(result.get("totalBookings")))
                    .totalRevenue(toBigDecimal(result.get("totalRevenue")))
                    .build());
        }

        return items;
    }

    private List<MovieShowcaseDTO> computeNowShowingMovies() {
        Query query = Query.query(Criteria.where("deletedAt").is(null)
                        .and("isActive").is(true)
                        .and("status").in("now_showing", "NOW_SHOWING"))
                .with(Sort.by(Sort.Direction.DESC, "releaseDate"))
                .limit(6);

        List<Document> docs = mongoTemplate.find(query, Document.class, MOVIES_COLLECTION);
        List<MovieShowcaseDTO> items = new ArrayList<>();

        for (Document doc : docs) {
            items.add(toMovieShowcaseDTO(doc));
        }

        return items;
    }

    private List<MovieShowcaseDTO> computeComingSoonMovies() {
        Query query = Query.query(Criteria.where("deletedAt").is(null)
                        .and("isActive").is(true)
                        .and("status").in("coming_soon", "COMING_SOON"))
                .with(Sort.by(Sort.Direction.ASC, "releaseDate"))
                .limit(6);

        List<Document> docs = mongoTemplate.find(query, Document.class, MOVIES_COLLECTION);
        List<MovieShowcaseDTO> items = new ArrayList<>();

        for (Document doc : docs) {
            items.add(toMovieShowcaseDTO(doc));
        }

        return items;
    }

    private List<RecentBookingDTO> computeRecentBookings() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("deletedAt").is(null)),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt")),
                Aggregation.limit(10),
                lookupByStringId("customerId", CUSTOMERS_COLLECTION, "customer", true, false),
                unwind("customer", true),
                lookupByStringId("showtimeId", SHOWTIMES_COLLECTION, "showtime", true, false),
                unwind("showtime", true),
                lookupByStringId("showtime.movieId", MOVIES_COLLECTION, "movie", true, false),
                unwind("movie", true),
                lookupByStringId("showtime.theatreId", THEATRES_COLLECTION, "theatre", true, false),
                unwind("theatre", true)
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, BOOKINGS_COLLECTION, Document.class);
        List<RecentBookingDTO> items = new ArrayList<>();

        for (Document result : results.getMappedResults()) {
            Integer seatStatusCode = toInteger(result.get("seatStatusCode"));

            items.add(RecentBookingDTO.builder()
                    .bookingReference(stringValue(result.get("bookingReference")))
                    .customerName(buildCustomerName(toDocument(result.get("customer"))))
                    .movieTitle(stringValue(field(toDocument(result.get("movie")), "title")))
                    .theatreName(stringValue(field(toDocument(result.get("theatre")), "name")))
                    .numberOfSeats(toInteger(result.get("numberOfSeats")))
                    .totalAmount(toBigDecimal(result.get("totalAmount")))
                    .paymentStatus(stringValue(result.get("paymentStatus")))
                    .seatStatusCode(seatStatusCode)
                    .seatStatusLabel(seatStatusLabel(seatStatusCode))
                    .bookingDate(formatDateTime(result.get("createdAt")))
                    .build());
        }

        return items;
    }

    private List<PaymentMethodDTO> computePaymentMethodDistribution() {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("deletedAt").is(null)
                .and("status").is("COMPLETED")
                .and("paymentMethod").ne(null)),
            projectPaymentMethodAndAmount(),
                Aggregation.group("paymentMethod")
                        .count().as("count")
                .sum("amount").as("amount"),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "amount")),
                Aggregation.project("count", "amount").and("_id").as("method")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, PAYMENTS_COLLECTION, Document.class);
        List<PaymentMethodDTO> items = new ArrayList<>();

        for (Document result : results.getMappedResults()) {
            items.add(PaymentMethodDTO.builder()
                    .method(stringValue(result.get("method")))
                    .count(toLong(result.get("count")))
                    .amount(toBigDecimal(result.get("amount")))
                    .build());
        }

        return items;
    }

    private AggregationOperation projectPaymentMethodAndAmount() {
        return context -> new Document("$project",
                new Document("paymentMethod", 1)
                        .append("amount", new Document("$convert",
                                new Document("input", "$amount")
                                        .append("to", "decimal")
                                        .append("onError", new Decimal128(BigDecimal.ZERO))
                                        .append("onNull", new Decimal128(BigDecimal.ZERO)))));
    }

    private BigDecimal sumAmount(String collectionName, Criteria criteria) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                addFieldsTotalAmountDouble(),
                Aggregation.group().sum("totalAmount").as("amount")
        );

        Document result = mongoTemplate.aggregate(aggregation, collectionName, Document.class).getUniqueMappedResult();
        if (result == null) {
            return BigDecimal.ZERO;
        }

        return toBigDecimal(result.get("amount"));
    }

    private MovieShowcaseDTO toMovieShowcaseDTO(Document doc) {
        return MovieShowcaseDTO.builder()
                .id(stringValue(doc.get("_id")))
                .title(stringValue(doc.get("title")))
                .poster(stringValue(doc.get("poster")))
                .duration(toInteger(doc.get("duration")))
                .certification(stringValue(doc.get("certification")))
                .language(toStringList(doc.get("language")))
                .releaseDate(formatDate(doc.get("releaseDate")))
                .build();
    }

    private String seatStatusLabel(Integer code) {
        if (code == null) {
            return "Unknown";
        }

        return switch (code) {
            case 3 -> "Pending";
            case 2 -> "Confirmed";
            case 1 -> "Cancelled";
            default -> "Unknown";
        };
    }

    /**
     * Convert totalAmount from string to double for proper numeric aggregation.
     * MongoDB stores totalAmount as string, but $sum on strings returns 0.
     * This $addFields stage converts totalAmount to a double so $sum works correctly.
     */
    private AggregationOperation addFieldsTotalAmountDouble() {
        return context -> new Document("$addFields",
                new Document("totalAmount",
                        new Document("$toDouble", "$totalAmount")));
    }

    private AggregationOperation projectBookingDateAndAmount() {
        return context -> new Document("$project",
                new Document("totalAmount",
                        new Document("$toDouble", "$totalAmount"))
                        .append("date", new Document("$dateToString",
                                new Document("format", "%Y-%m-%d")
                                        .append("date", "$createdAt")
                                        .append("timezone", "Asia/Kathmandu"))));
    }

    private AggregationOperation unwind(String field, boolean preserveNullAndEmptyArrays) {
        return context -> new Document("$unwind",
                new Document("path", "$" + field)
                        .append("preserveNullAndEmptyArrays", preserveNullAndEmptyArrays));
    }

    private AggregationOperation lookupByStringId(
            String localFieldPath,
            String fromCollection,
            String asField,
            boolean filterDeletedAtNull,
            boolean filterActiveTrue) {

        return context -> {
            List<Document> andConditions = new ArrayList<>();

            // Compare IDs by string value so lookups still work when one side is ObjectId and the other is String.
            andConditions.add(new Document("$eq", List.of(
                    new Document("$toString", "$_id"),
                    new Document("$toString", "$$localId")
            )));

            if (filterDeletedAtNull) {
                andConditions.add(new Document("$eq", Arrays.asList("$deletedAt", null)));
            }

            if (filterActiveTrue) {
                andConditions.add(new Document("$eq", List.of("$isActive", true)));
            }

            Document exprCondition = andConditions.size() == 1
                    ? andConditions.get(0)
                    : new Document("$and", andConditions);

            Document pipelineMatch = new Document("$match", new Document("$expr", exprCondition));

            return new Document("$lookup", new Document("from", fromCollection)
                    .append("let", new Document("localId", "$" + localFieldPath))
                    .append("pipeline", List.of(pipelineMatch))
                    .append("as", asField));
        };
    }

    private String buildCustomerName(Document customerDoc) {
        if (customerDoc == null || customerDoc.isEmpty()) {
            return "Unknown Customer";
        }

        String legacyName = stringValue(customerDoc.get("name"));
        if (legacyName != null && !legacyName.isBlank()) {
            return legacyName;
        }

        String firstName = stringValue(customerDoc.get("first_name"));
        String middleName = stringValue(customerDoc.get("middle_name"));
        String lastName = stringValue(customerDoc.get("last_name"));

        List<String> parts = new ArrayList<>();
        if (firstName != null && !firstName.isBlank()) {
            parts.add(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            parts.add(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            parts.add(lastName.trim());
        }

        if (parts.isEmpty()) {
            return "Unknown Customer";
        }

        return String.join(" ", parts);
    }

    private Object field(Document document, String key) {
        if (document == null) {
            return null;
        }
        return document.get(key);
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Decimal128 decimal128) {
            return decimal128.bigDecimalValue();
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private List<String> toStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }

        return List.of(value.toString());
    }

    private String formatDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.format(DATE_FORMATTER);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate().format(DATE_FORMATTER);
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER);
        }

        String text = value.toString();
        if (text.length() >= 10 && text.charAt(4) == '-' && text.charAt(7) == '-') {
            return text.substring(0, 10);
        }
        return text;
    }

    private String formatDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATETIME_FORMATTER);
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATETIME_FORMATTER);
        }
        return value.toString();
    }

    private Document toDocument(Object value) {
        if (value instanceof Document document) {
            return document;
        }
        if (value instanceof Map<?, ?> map) {
            Document document = new Document();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                document.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return document;
        }
        return null;
    }
}