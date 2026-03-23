package com.example.library.inventory;

import java.time.Instant;

import com.example.library.branch.LibraryBranch;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_copy")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holding_id", nullable = false)
    private BookHolding holding;

    @Column(nullable = false, length = 80)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookCopyStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_branch_id", nullable = false)
    private LibraryBranch currentBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id")
    private LibraryLocation currentLocation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookCopy() {
    }

    public BookCopy(BookHolding holding, String barcode, BookCopyStatus status) {
        this.holding = requireHolding(holding);
        this.barcode = normalizeBarcode(barcode);
        this.status = requireStatus(status);
        moveHome();
        if (status == BookCopyStatus.IN_TRANSIT || status == BookCopyStatus.BORROWED) {
            this.currentLocation = null;
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BookHolding getHolding() {
        return holding;
    }

    public String getBarcode() {
        return barcode;
    }

    public BookCopyStatus getStatus() {
        return status;
    }

    public LibraryBranch getCurrentBranch() {
        return currentBranch;
    }

    public LibraryLocation getCurrentLocation() {
        return currentLocation;
    }

    public boolean isAvailable() {
        return status.countsAsAvailable();
    }

    public boolean isMutableForManualInventory() {
        return status.canBeAdjustedManually();
    }

    public void markBorrowed() {
        if (status != BookCopyStatus.AVAILABLE && status != BookCopyStatus.RESERVED_FOR_PICKUP) {
            throw new IllegalArgumentException("Only available or reserved copies can be borrowed");
        }
        this.status = BookCopyStatus.BORROWED;
        this.currentLocation = null;
    }

    public void markUnavailable() {
        if (status != BookCopyStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available copies can be marked unavailable");
        }
        this.status = BookCopyStatus.UNAVAILABLE;
        moveHome();
    }

    public void markAvailableAtHome() {
        this.status = BookCopyStatus.AVAILABLE;
        moveHome();
    }

    public void markReadyForPickup(LibraryBranch destinationBranch) {
        this.status = BookCopyStatus.RESERVED_FOR_PICKUP;
        this.currentBranch = requireBranch(destinationBranch);
        this.currentLocation = null;
    }

    public void markInTransit(LibraryBranch destinationBranch) {
        this.status = BookCopyStatus.IN_TRANSIT;
        this.currentBranch = requireBranch(destinationBranch);
        this.currentLocation = null;
    }

    public void markClaimedReturned() {
        if (status != BookCopyStatus.BORROWED) {
            throw new IllegalArgumentException("Only borrowed copies can be marked claimed returned");
        }
        this.status = BookCopyStatus.CLAIMED_RETURNED;
    }

    public void markLost() {
        if (status != BookCopyStatus.BORROWED && status != BookCopyStatus.CLAIMED_RETURNED) {
            throw new IllegalArgumentException("Only borrowed copies can be marked lost");
        }
        this.status = BookCopyStatus.LOST;
        this.currentLocation = null;
    }

    public void markDamaged() {
        if (status != BookCopyStatus.BORROWED && status != BookCopyStatus.CLAIMED_RETURNED) {
            throw new IllegalArgumentException("Only borrowed copies can be marked damaged");
        }
        this.status = BookCopyStatus.DAMAGED;
        this.currentLocation = null;
    }

    public void rebaseHomeLocation() {
        if (status == BookCopyStatus.AVAILABLE || status == BookCopyStatus.UNAVAILABLE) {
            moveHome();
        }
    }

    private void moveHome() {
        this.currentBranch = requireBranch(holding.getBranch());
        this.currentLocation = holding.getLocation();
    }

    private BookHolding requireHolding(BookHolding value) {
        if (value == null) {
            throw new IllegalArgumentException("Copy holding is required");
        }
        return value;
    }

    private BookCopyStatus requireStatus(BookCopyStatus value) {
        if (value == null) {
            throw new IllegalArgumentException("Copy status is required");
        }
        return value;
    }

    private LibraryBranch requireBranch(LibraryBranch value) {
        if (value == null) {
            throw new IllegalArgumentException("Current branch is required");
        }
        return value;
    }

    private String normalizeBarcode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Copy barcode is required");
        }
        return value.trim().toUpperCase();
    }
}
