package com.awal.cineq.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingRequest {

    private String paymentReference; // optional
    private String paymentMethod;    // optional
}
