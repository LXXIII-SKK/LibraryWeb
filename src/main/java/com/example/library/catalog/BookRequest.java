package com.example.library.catalog;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @Size(max = 100) String category,
        @Size(max = 50) String isbn,
        List<@Size(max = 50) String> tags) {
}
