package com.bankapp.portal.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImpersonationRequest {

    @NotBlank(message = "Target merchant identifier is a mandatory requirement.")
    private String targetMerchantId;

    /**
     * Module 3 Constraint: Mandatory justification tracking reason.
     * Enforces a minimum length boundary directly at the API gateway layer.
     */
    @NotBlank(message = "A valid reason must be provided to establish an impersonation proxy session.")
    @Size(min = 10, max = 500, message = "Justification reason context must be between 10 and 500 characters.")
    private String reason;
}