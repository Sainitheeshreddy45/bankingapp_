package com.bankapp.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequests {

    @Data
    public static class SignUpRequest {
//        @NotBlank @Size(min = 4, max = 50)
//        private String username;
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 8, max = 100)
        private String password;

        private String targetRole; // e.g. "MERCHANT_OWNER", "RISK_ANALYST"
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }
}