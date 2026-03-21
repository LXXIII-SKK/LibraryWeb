package com.example.library.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchUpsertRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 255) String address,
        @Size(max = 50) String phone,
        boolean active) {
}
