package com.bankapp.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotNull
    private String transactionId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Invalid payout range limits processing bounds.")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^\\d{16}$", message = "Invalid payment instrument format mapping.")
    private String rawPanMock; // Tokenized or safely discarded at boundaries
}