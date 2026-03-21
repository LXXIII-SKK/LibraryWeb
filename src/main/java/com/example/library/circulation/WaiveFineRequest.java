package com.example.library.circulation;

import jakarta.validation.constraints.Size;

public record WaiveFineRequest(@Size(max = 255) String note) {
}
