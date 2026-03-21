package com.example.library.notification;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.example.library.branch.LibraryBranch;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_notification")
public class StaffNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 600)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private AppUser targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private LibraryBranch branch;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "staff_notification_target_role", joinColumns = @JoinColumn(name = "notification_id"))
    @Column(name = "target_role", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<AppRole> targetRoles = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StaffNotification() {
    }

    public StaffNotification(
            String title,
            String message,
            AppUser createdByUser,
            AppUser targetUser,
            LibraryBranch branch,
            Set<AppRole> targetRoles) {
        this.title = normalizeRequired(title, "Notification title");
        this.message = normalizeRequired(message, "Notification message");
        this.createdByUser = requireUser(createdByUser);
        this.targetUser = targetUser;
        this.branch = branch;
        this.targetRoles = normalizeRoles(targetRoles);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public AppUser getCreatedByUser() {
        return createdByUser;
    }

    public AppUser getTargetUser() {
        return targetUser;
    }

    public LibraryBranch getBranch() {
        return branch;
    }

    public Set<AppRole> getTargetRoles() {
        return Set.copyOf(targetRoles);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private AppUser requireUser(AppUser createdByUser) {
        if (createdByUser == null) {
            throw new IllegalArgumentException("Notification creator is required");
        }
        return createdByUser;
    }

    private Set<AppRole> normalizeRoles(Set<AppRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one target role is required");
        }
        return new LinkedHashSet<>(roles);
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }
}
