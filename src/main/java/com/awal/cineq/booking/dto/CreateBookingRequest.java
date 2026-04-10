package com.awal.cineq.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotBlank(message = "Showtime ID is required")
    private String showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 10, message = "Cannot book more than 10 seats at once")
    private List<SeatRequest> seats;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // "ESEWA", "KHALTI", "CONNECTIPS"
}
