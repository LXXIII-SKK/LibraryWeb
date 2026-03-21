package com.example.library.upcoming;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.library.branch.LibraryBranch;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "upcoming_book")
public class UpcomingBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String isbn;

    @Column(length = 600)
    private String summary;

    @Column(name = "expected_at", nullable = false)
    private Instant expectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private LibraryBranch branch;

    @OneToMany(mappedBy = "upcomingBook", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final Set<UpcomingBookTag> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UpcomingBook() {
    }

    public UpcomingBook(
            String title,
            String author,
            String category,
            String isbn,
            String summary,
            Instant expectedAt,
            LibraryBranch branch,
            Collection<String> tags) {
        this.title = normalizeRequired(title, "Upcoming book title");
        this.author = normalizeRequired(author, "Upcoming book author");
        this.category = normalizeNullable(category);
        this.isbn = normalizeNullable(isbn);
        this.summary = normalizeNullable(summary);
        this.expectedAt = requireExpectedAt(expectedAt);
        this.branch = branch;
        replaceTags(tags);
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

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getExpectedAt() {
        return expectedAt;
    }

    public LibraryBranch getBranch() {
        return branch;
    }

    public List<String> getTags() {
        return tags.stream()
                .map(UpcomingBookTag::getName)
                .sorted()
                .toList();
    }

    public void update(
            String title,
            String author,
            String category,
            String isbn,
            String summary,
            Instant expectedAt,
            LibraryBranch branch,
            Collection<String> tags) {
        this.title = normalizeRequired(title, "Upcoming book title");
        this.author = normalizeRequired(author, "Upcoming book author");
        this.category = normalizeNullable(category);
        this.isbn = normalizeNullable(isbn);
        this.summary = normalizeNullable(summary);
        this.expectedAt = requireExpectedAt(expectedAt);
        this.branch = branch;
        replaceTags(tags);
    }

    private Instant requireExpectedAt(Instant expectedAt) {
        if (expectedAt == null) {
            throw new IllegalArgumentException("Expected arrival date is required");
        }
        return expectedAt;
    }

    private void replaceTags(Collection<String> tagValues) {
        tags.clear();
        if (tagValues == null) {
            return;
        }
        tagValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .forEach(value -> tags.add(new UpcomingBookTag(this, value)));
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
