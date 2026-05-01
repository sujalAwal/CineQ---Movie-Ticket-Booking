package com.awal.cineq.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response wrapper for payment listings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPageResponse {

    private List<PaymentListDTO> payments;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
