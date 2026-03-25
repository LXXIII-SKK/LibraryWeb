package com.example.library.circulation;

import java.time.Instant;

import com.example.library.catalog.Book;
import com.example.library.identity.AppUser;
import com.example.library.inventory.BookCopy;
import com.example.library.inventory.BookHolding;
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
@Table(name = "borrow_transaction")
public class BorrowTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id")
    private BookHolding holding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @Column(name = "borrowed_at", nullable = false)
    private Instant borrowedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "last_renewed_at")
    private Instant lastRenewedAt;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount;

    @Column(name = "last_renewal_override", nullable = false)
    private boolean lastRenewalOverride;

    @Column(name = "last_renewal_reason", length = 255)
    private String lastRenewalReason;

    @Column(name = "exception_note", length = 500)
    private String exceptionNote;

    @Column(name = "exception_recorded_at")
    private Instant exceptionRecordedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BorrowStatus status;

    protected BorrowTransaction() {
    }

    public BorrowTransaction(AppUser user, Book book, Instant borrowedAt, Instant dueAt) {
        this(user, book, null, null, borrowedAt, dueAt);
    }

    public BorrowTransaction(AppUser user, Book book, BookHolding holding, Instant borrowedAt, Instant dueAt) {
        this(user, book, holding, null, borrowedAt, dueAt);
    }

    public BorrowTransaction(AppUser user, Book book, BookHolding holding, BookCopy copy, Instant borrowedAt, Instant dueAt) {
        this.user = user;
        this.book = book;
        this.holding = holding;
        this.copy = copy;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.status = BorrowStatus.BORROWED;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Book getBook() {
        return book;
    }

    public BookHolding getHolding() {
        return holding;
    }

    public BookCopy getCopy() {
        return copy;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public Instant getLastRenewedAt() {
        return lastRenewedAt;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public boolean isLastRenewalOverride() {
        return lastRenewalOverride;
    }

    public String getLastRenewalReason() {
        return lastRenewalReason;
    }

    public String getExceptionNote() {
        return exceptionNote;
    }

    public Instant getExceptionRecordedAt() {
        return exceptionRecordedAt;
    }

    public BorrowStatus getStatus() {
        return status;
    }

    public void markReturned(Instant returnedAt) {
        if (!status.canReturnToInventory()) {
            throw new IllegalArgumentException("Only borrowed or claimed-returned items can be returned");
        }
        this.returnedAt = returnedAt;
        this.exceptionNote = null;
        this.exceptionRecordedAt = null;
        this.status = BorrowStatus.RETURNED;
    }

    public void renewTo(Instant dueAt, Instant renewedAt, boolean override, String renewalReason) {
        if (!status.isRenewable()) {
            throw new IllegalArgumentException("Only borrowed items can be renewed");
        }
        this.dueAt = dueAt;
        this.lastRenewedAt = renewedAt;
        this.renewalCount += 1;
        this.lastRenewalOverride = override;
        this.lastRenewalReason = renewalReason;
    }

    public void recordException(BorrowStatus status, String note, Instant recordedAt) {
        if (this.status != BorrowStatus.BORROWED) {
            throw new IllegalArgumentException("Only borrowed items can be marked with an exception");
        }
        if (status == null || !status.isExceptionState()) {
            throw new IllegalArgumentException("A borrowing exception status is required");
        }
        this.status = status;
        this.exceptionNote = note;
        this.exceptionRecordedAt = recordedAt;
    }
}
