package com.awal.cineq.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingDetailResponse {

    private String seatName;        // e.g. "G3"
    private String row;             // e.g. "G"
    private Integer col;            // e.g. 3
    private String seatCode;        // e.g. "P" — type code
    private Double seatPrice;
    private Integer seatStatusCode; // from seat_statuses: 3=Reserved, 2=Booked
    private String seatStatusName;  // e.g. "Reserved"
    private String seatStatusColor; // e.g. "#FFA500"
}
