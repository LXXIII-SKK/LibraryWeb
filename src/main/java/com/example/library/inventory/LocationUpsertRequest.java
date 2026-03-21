package com.example.library.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LocationUpsertRequest(
        @NotNull Long branchId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 50) String floorLabel,
        @Size(max = 100) String zoneLabel,
        boolean active) {
}
