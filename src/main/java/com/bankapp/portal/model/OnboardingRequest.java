package com.bankapp.portal.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRequest {

    /**
     * Captures the legal trade name of the business entity.
     * Guarded against common XSS payload length overflow profiles.
     */
    @NotBlank(message = "Business name is a mandatory field.")
    @Size(min = 2, max = 150, message = "Business name must be between 2 and 150 characters.")
    private String businessName;

    /**
     * Captures the target settlement bank account routing endpoint.
     * Validates that only numeric digits are passed, stripping risky special characters.
     */
    @NotBlank(message = "Bank account number is a mandatory field.")
    @Pattern(regexp = "^\\d{9,18}$", message = "Invalid account number format. Must contain between 9 and 18 numeric digits.")
    private String bankAccountNumber;

    /**
     * Captures the standard Indian Financial System Code (IFSC).
     * Enforces strict compliance checks mapping directly to the RBI format structure.
     */
    @NotBlank(message = "Bank IFSC code is a mandatory field.")
    @Pattern(
            regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
            message = "Invalid IFSC format. Must follow standard format (e.g., SBIN0012345): 4 letters, followed by a zero, then 6 alphanumeric characters."
    )
    private String bankIfsc;
}