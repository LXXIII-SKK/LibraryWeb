package com.example.library.identity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.example.library.branch.BranchSummaryResponse;

public record AccessOptionsResponse(
        List<AppRole> roles,
        List<AccountStatus> accountStatuses,
        List<MembershipStatus> membershipStatuses,
        List<BranchSummaryResponse> branches,
        List<UserDisciplineActionType> disciplineActions,
        List<UserDisciplineReason> disciplineReasons) {

    static AccessOptionsResponse defaultOptions(List<BranchSummaryResponse> branches) {
        return new AccessOptionsResponse(
                List.of(AppRole.values()),
                List.of(AccountStatus.values()),
                List.of(MembershipStatus.values()),
                branches,
                List.of(UserDisciplineActionType.values()),
                List.of(UserDisciplineReason.values()));
    }

    static AccessOptionsResponse lockedTo(UserAccessResponse user) {
        return new AccessOptionsResponse(
                List.of(user.role()),
                List.of(user.accountStatus()),
                List.of(user.membershipStatus()),
                Stream.of(user.branch(), user.homeBranch())
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList(),
                List.of(),
                List.of());
    }
}
