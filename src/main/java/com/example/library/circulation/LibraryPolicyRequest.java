package com.example.library.circulation;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LibraryPolicyRequest(
        @Min(1) int standardLoanDays,
        @Min(1) int renewalDays,
        @Min(0) int maxRenewals,
        @NotNull @DecimalMin("0.00") BigDecimal finePerOverdueDay,
        @NotNull @DecimalMin("0.00") BigDecimal fineWaiverLimit,
        boolean allowRenewalWithActiveReservations) {
}
