package com.awal.cineq.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent by the frontend after eSewa redirects back with ?data=<base64>.
 * The frontend extracts the data query param and posts it here for server-side verification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsewaVerifyRequest {

    @NotBlank(message = "eSewa response data is required")
    private String data; // base64-encoded JSON from eSewa redirect URL
}
