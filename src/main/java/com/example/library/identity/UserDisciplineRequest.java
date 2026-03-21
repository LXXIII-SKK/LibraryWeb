package com.example.library.identity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDisciplineRequest(
        @NotNull UserDisciplineActionType action,
        @NotNull UserDisciplineReason reason,
        @Size(max = 500) String note) {
}
