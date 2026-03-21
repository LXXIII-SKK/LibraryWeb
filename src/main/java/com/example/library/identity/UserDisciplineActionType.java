package com.example.library.identity;

public enum UserDisciplineActionType {
    SUSPEND(AccountStatus.SUSPENDED),
    BAN(AccountStatus.LOCKED),
    REINSTATE(AccountStatus.ACTIVE);

    private final AccountStatus resultingAccountStatus;

    UserDisciplineActionType(AccountStatus resultingAccountStatus) {
        this.resultingAccountStatus = resultingAccountStatus;
    }

    public AccountStatus resultingAccountStatus() {
        return resultingAccountStatus;
    }
}
