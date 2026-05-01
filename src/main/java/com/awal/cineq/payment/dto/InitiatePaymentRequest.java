package com.awal.cineq.payment.dto;

import com.awal.cineq.payment.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {

    @NotBlank(message = "Showtime ID is required")
    private String showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(min = 1, max = 10, message = "You can book between 1 and 10 seats at a time")
    @Valid
    private List<SeatSelection> seats;

    @NotNull(message = "Payment method is required (ESEWA, KHALTI, CONNECTIPS)")
    private PaymentMethod paymentMethod;
}
