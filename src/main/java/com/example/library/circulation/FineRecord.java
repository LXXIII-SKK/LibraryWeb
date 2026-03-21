package com.example.library.circulation;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.library.identity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fine_record")
public class FineRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrow_transaction_id")
    private BorrowTransaction borrowTransaction;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FineStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private AppUser resolvedByUser;

    @Column(name = "resolution_note", length = 255)
    private String resolutionNote;

    protected FineRecord() {
    }

    public FineRecord(AppUser user, BorrowTransaction borrowTransaction, BigDecimal amount, String reason, Instant createdAt) {
        this.user = user;
        this.borrowTransaction = borrowTransaction;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = FineStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public BorrowTransaction getBorrowTransaction() {
        return borrowTransaction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public FineStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public AppUser getResolvedByUser() {
        return resolvedByUser;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void waive(AppUser resolvedByUser, String resolutionNote, Instant resolvedAt) {
        if (status != FineStatus.OPEN) {
            throw new IllegalArgumentException("Only open fines can be waived");
        }
        this.status = FineStatus.WAIVED;
        this.resolvedByUser = resolvedByUser;
        this.resolutionNote = resolutionNote;
        this.resolvedAt = resolvedAt;
    }
}
