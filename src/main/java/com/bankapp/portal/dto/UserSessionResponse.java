package com.bankapp.portal.dto;

import java.util.Set;

public record UserSessionResponse(
        UserPayload user,
        boolean isImpersonating
) {
    public record UserPayload(
            String email,
            String role,
            Set<String> permissions,
            String kycStatus,
            String businessName,
            String companyType,
            String accountNumber,
            String ifscCode,
            String directorDetails,
            String panStatus,
            String gstStatus
    ) {}
}