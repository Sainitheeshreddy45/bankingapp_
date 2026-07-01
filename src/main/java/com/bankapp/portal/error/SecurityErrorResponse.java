package com.bankapp.portal.error;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SecurityErrorResponse {
    private String message;
    private String errorCode; // 💎 The magic key: "SESSION_EXPIRED"
}
