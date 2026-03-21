package com.example.library.identity;

import java.time.Instant;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "user_discipline_record")
public class UserDisciplineRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id")
    private AppUser targetUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id")
    private AppUser actorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private UserDisciplineActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 50)
    private UserDisciplineReason reasonCode;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_account_status", nullable = false, length = 30)
    private AccountStatus previousAccountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_account_status", nullable = false, length = 30)
    private AccountStatus resultingAccountStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserDisciplineRecord() {
    }

    public UserDisciplineRecord(
            AppUser targetUser,
            AppUser actorUser,
            UserDisciplineActionType actionType,
            UserDisciplineReason reasonCode,
            String note,
            AccountStatus previousAccountStatus,
            AccountStatus resultingAccountStatus) {
        this.targetUser = targetUser;
        this.actorUser = actorUser;
        this.actionType = actionType;
        this.reasonCode = reasonCode;
        this.note = normalizeNullable(note);
        this.previousAccountStatus = previousAccountStatus;
        this.resultingAccountStatus = resultingAccountStatus;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getTargetUser() {
        return targetUser;
    }

    public AppUser getActorUser() {
        return actorUser;
    }

    public UserDisciplineActionType getActionType() {
        return actionType;
    }

    public UserDisciplineReason getReasonCode() {
        return reasonCode;
    }

    public String getNote() {
        return note;
    }

    public AccountStatus getPreviousAccountStatus() {
        return previousAccountStatus;
    }

    public AccountStatus getResultingAccountStatus() {
        return resultingAccountStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
