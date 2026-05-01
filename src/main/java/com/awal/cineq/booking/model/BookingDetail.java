package com.awal.cineq.booking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Embedded document for each seat within a Booking.
 * Field names mirror showtime.seatLayout[] exactly so queries are consistent.
 *
 * seatStatusCode references seat_statuses collection:
 *   3 = Reserved, 2 = Booked, 1 = Available
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {

    private String seatName;        // e.g. "G3"
    private String row;             // e.g. "G"
    private Integer col;            // e.g. 3
    private String seatCode;        // e.g. "P" — type code from seat_types collection
    private BigDecimal seatPrice;   // from showtime.seatLayout[].price (server-side)
    private Integer seatStatusCode; // from seat_statuses: 3=Reserved, 2=Booked, 1=Available
}
