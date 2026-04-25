package com.awal.cineq.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueTrendItemDTO {

    private String date;
    private BigDecimal revenue;
    private long bookings;
}
