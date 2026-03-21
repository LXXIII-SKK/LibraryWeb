package com.example.library.upcoming;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpcomingBookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @Size(max = 100) String category,
        @Size(max = 50) String isbn,
        @Size(max = 600) String summary,
        @NotNull @Future Instant expectedAt,
        Long branchId,
        List<@Size(max = 50) String> tags) {
}
