package com.example.library.inventory;

import java.time.Instant;

import com.example.library.branch.LibraryBranch;
import com.example.library.catalog.Book;
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
@Table(name = "book_holding")
public class BookHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private LibraryBranch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LibraryLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldingFormat format;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "access_url", length = 500)
    private String accessUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookHolding() {
    }

    public BookHolding(
            Book book,
            LibraryBranch branch,
            LibraryLocation location,
            HoldingFormat format,
            int totalQuantity,
            int availableQuantity,
            String accessUrl,
            boolean active) {
        this.book = requireBook(book);
        this.branch = requireBranch(branch);
        this.location = location;
        this.format = requireFormat(format);
        this.totalQuantity = validateTotal(totalQuantity);
        this.availableQuantity = validateAvailable(totalQuantity, availableQuantity);
        this.accessUrl = normalizeAccessUrl(format, accessUrl);
        this.active = active;
        validateLocation(location, branch, format);
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

    public Book getBook() {
        return book;
    }

    public LibraryBranch getBranch() {
        return branch;
    }

    public LibraryLocation getLocation() {
        return location;
    }

    public HoldingFormat getFormat() {
        return format;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasOnlineAccess() {
        return format == HoldingFormat.DIGITAL && accessUrl != null;
    }

    public boolean isBorrowable() {
        return active && availableQuantity > 0;
    }

    public void update(
            LibraryBranch branch,
            LibraryLocation location,
            HoldingFormat format,
            int totalQuantity,
            int availableQuantity,
            String accessUrl,
            boolean active) {
        this.branch = requireBranch(branch);
        this.location = location;
        this.format = requireFormat(format);
        this.totalQuantity = validateTotal(totalQuantity);
        this.availableQuantity = validateAvailable(totalQuantity, availableQuantity);
        String nextAccessUrl = accessUrl;
        if (format == HoldingFormat.DIGITAL && (nextAccessUrl == null || nextAccessUrl.isBlank()) && this.accessUrl != null) {
            nextAccessUrl = this.accessUrl;
        }
        this.accessUrl = normalizeAccessUrl(format, nextAccessUrl);
        this.active = active;
        validateLocation(location, branch, format);
    }

    public void borrowOne() {
        if (!active || availableQuantity <= 0) {
            throw new IllegalArgumentException("This holding is not available for borrowing");
        }
        this.availableQuantity -= 1;
    }

    public void returnOne() {
        if (availableQuantity >= totalQuantity) {
            throw new IllegalArgumentException("This holding is already fully available");
        }
        this.availableQuantity += 1;
    }

    private Book requireBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book is required");
        }
        return book;
    }

    private LibraryBranch requireBranch(LibraryBranch branch) {
        if (branch == null) {
            throw new IllegalArgumentException("Holding branch is required");
        }
        return branch;
    }

    private HoldingFormat requireFormat(HoldingFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("Holding format is required");
        }
        return format;
    }

    private int validateTotal(int totalQuantity) {
        if (totalQuantity < 0) {
            throw new IllegalArgumentException("Total quantity must be zero or greater");
        }
        return totalQuantity;
    }

    private int validateAvailable(int totalQuantity, int availableQuantity) {
        if (availableQuantity < 0 || availableQuantity > totalQuantity) {
            throw new IllegalArgumentException("Available quantity must be between zero and total quantity");
        }
        return availableQuantity;
    }

    private String normalizeAccessUrl(HoldingFormat format, String accessUrl) {
        String normalized = accessUrl == null || accessUrl.isBlank() ? null : accessUrl.trim();
        if (format == HoldingFormat.DIGITAL && normalized == null) {
            throw new IllegalArgumentException("Digital holdings require an access URL");
        }
        if (format == HoldingFormat.PHYSICAL) {
            return null;
        }
        return normalized;
    }

    private void validateLocation(LibraryLocation location, LibraryBranch branch, HoldingFormat format) {
        if (format == HoldingFormat.PHYSICAL && location == null) {
            throw new IllegalArgumentException("Physical holdings require a shelf location");
        }
        if (location != null && location.getBranch() != null && !location.getBranch().getId().equals(branch.getId())) {
            throw new IllegalArgumentException("Holding location must belong to the same branch");
        }
    }
}
