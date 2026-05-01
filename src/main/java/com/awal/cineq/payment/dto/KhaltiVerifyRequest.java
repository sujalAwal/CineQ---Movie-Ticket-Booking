package com.awal.cineq.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent by the frontend after Khalti redirects back with ?pidx=<token>.
 * The frontend extracts the pidx query param and posts it here for server-side verification
 * via the Khalti lookup API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KhaltiVerifyRequest {

    @NotBlank(message = "Khalti pidx is required")
    private String pidx;
}
