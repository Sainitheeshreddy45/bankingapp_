package com.bankapp.portal.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSearchCriteria {
    private String merchantId;
    private String status;
    private String paymentMode;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int page;
    private int size;
}