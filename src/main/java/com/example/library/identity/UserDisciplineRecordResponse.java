package com.example.library.identity;

import java.time.Instant;

public record UserDisciplineRecordResponse(
        Long id,
        Long targetUserId,
        String targetUsername,
        Long actorUserId,
        String actorUsername,
        UserDisciplineActionType action,
        UserDisciplineReason reason,
        String note,
        AccountStatus previousAccountStatus,
        AccountStatus resultingAccountStatus,
        Instant createdAt) {

    static UserDisciplineRecordResponse from(UserDisciplineRecord record) {
        return new UserDisciplineRecordResponse(
                record.getId(),
                record.getTargetUser().getId(),
                record.getTargetUser().getUsername(),
                record.getActorUser().getId(),
                record.getActorUser().getUsername(),
                record.getActionType(),
                record.getReasonCode(),
                record.getNote(),
                record.getPreviousAccountStatus(),
                record.getResultingAccountStatus(),
                record.getCreatedAt());
    }
}
