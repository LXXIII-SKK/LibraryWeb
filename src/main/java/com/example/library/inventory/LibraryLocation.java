package com.example.library.inventory;

import java.time.Instant;

import com.example.library.branch.LibraryBranch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "library_location")
public class LibraryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private LibraryBranch branch;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "floor_label", length = 50)
    private String floorLabel;

    @Column(name = "zone_label", length = 100)
    private String zoneLabel;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LibraryLocation() {
    }

    private LibraryLocation(Long id) {
        this.id = id;
    }

    public LibraryLocation(
            LibraryBranch branch,
            String code,
            String name,
            String floorLabel,
            String zoneLabel,
            boolean active) {
        this.branch = requireBranch(branch);
        this.code = normalizeRequired(code, "Location code").toUpperCase();
        this.name = normalizeRequired(name, "Location name");
        this.floorLabel = normalizeNullable(floorLabel);
        this.zoneLabel = normalizeNullable(zoneLabel);
        this.active = active;
    }

    public static LibraryLocation reference(Long id) {
        return id == null ? null : new LibraryLocation(id);
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

    public LibraryBranch getBranch() {
        return branch;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getFloorLabel() {
        return floorLabel;
    }

    public String getZoneLabel() {
        return zoneLabel;
    }

    public boolean isActive() {
        return active;
    }

    public void updateDetails(
            LibraryBranch branch,
            String code,
            String name,
            String floorLabel,
            String zoneLabel,
            boolean active) {
        this.branch = requireBranch(branch);
        this.code = normalizeRequired(code, "Location code").toUpperCase();
        this.name = normalizeRequired(name, "Location name");
        this.floorLabel = normalizeNullable(floorLabel);
        this.zoneLabel = normalizeNullable(zoneLabel);
        this.active = active;
    }

    private LibraryBranch requireBranch(LibraryBranch branch) {
        if (branch == null) {
            throw new IllegalArgumentException("Location branch is required");
        }
        return branch;
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
