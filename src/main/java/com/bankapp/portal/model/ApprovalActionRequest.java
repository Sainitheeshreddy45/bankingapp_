package com.bankapp.portal.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalActionRequest {

    /**
     * Enforces that the incoming status modification strictly matches
     * the evaluation lifecycle state machine values.
     */
    @NotBlank(message = "Action status is a mandatory field.")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "Invalid execution status. Permitted actions are APPROVED or REJECTED.")
    private String status;
}