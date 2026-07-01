package com.bankapp.portal.dto;


import lombok.Data;

@Data
public class TwoFaRequest {
    String challengeId;
    String Otp;
}
