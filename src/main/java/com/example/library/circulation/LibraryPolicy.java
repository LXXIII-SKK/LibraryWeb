package com.example.library.circulation;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "library_policy")
public class LibraryPolicy {

    @Id
    private Long id;

    @Column(name = "standard_loan_days", nullable = false)
    private int standardLoanDays;

    @Column(name = "renewal_days", nullable = false)
    private int renewalDays;

    @Column(name = "max_renewals", nullable = false)
    private int maxRenewals;

    @Column(name = "fine_per_overdue_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal finePerOverdueDay;

    @Column(name = "fine_waiver_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineWaiverLimit;

    @Column(name = "allow_renewal_with_active_reservations", nullable = false)
    private boolean allowRenewalWithActiveReservations;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LibraryPolicy() {
    }

    public LibraryPolicy(
            Long id,
            int standardLoanDays,
            int renewalDays,
            int maxRenewals,
            BigDecimal finePerOverdueDay,
            BigDecimal fineWaiverLimit,
            boolean allowRenewalWithActiveReservations) {
        this.id = id;
        this.standardLoanDays = standardLoanDays;
        this.renewalDays = renewalDays;
        this.maxRenewals = maxRenewals;
        this.finePerOverdueDay = finePerOverdueDay;
        this.fineWaiverLimit = fineWaiverLimit;
        this.allowRenewalWithActiveReservations = allowRenewalWithActiveReservations;
    }

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public int getStandardLoanDays() {
        return standardLoanDays;
    }

    public int getRenewalDays() {
        return renewalDays;
    }

    public int getMaxRenewals() {
        return maxRenewals;
    }

    public BigDecimal getFinePerOverdueDay() {
        return finePerOverdueDay;
    }

    public BigDecimal getFineWaiverLimit() {
        return fineWaiverLimit;
    }

    public boolean isAllowRenewalWithActiveReservations() {
        return allowRenewalWithActiveReservations;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateFrom(LibraryPolicyRequest request) {
        this.standardLoanDays = request.standardLoanDays();
        this.renewalDays = request.renewalDays();
        this.maxRenewals = request.maxRenewals();
        this.finePerOverdueDay = request.finePerOverdueDay();
        this.fineWaiverLimit = request.fineWaiverLimit();
        this.allowRenewalWithActiveReservations = request.allowRenewalWithActiveReservations();
    }
}
