package com.example.library.circulation;

import java.math.BigDecimal;
import java.time.Instant;

public record LibraryPolicyResponse(
        int standardLoanDays,
        int renewalDays,
        int maxRenewals,
        BigDecimal finePerOverdueDay,
        BigDecimal fineWaiverLimit,
        boolean allowRenewalWithActiveReservations,
        Instant updatedAt) {

    static LibraryPolicyResponse from(LibraryPolicy policy) {
        return new LibraryPolicyResponse(
                policy.getStandardLoanDays(),
                policy.getRenewalDays(),
                policy.getMaxRenewals(),
                policy.getFinePerOverdueDay(),
                policy.getFineWaiverLimit(),
                policy.isAllowRenewalWithActiveReservations(),
                policy.getUpdatedAt());
    }
}
