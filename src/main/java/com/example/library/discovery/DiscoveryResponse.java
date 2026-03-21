package com.example.library.discovery;

import java.util.List;

public record DiscoveryResponse(
        List<DiscoveryBookResponse> recommendations,
        List<DiscoveryBookResponse> mostBorrowedThisWeek,
        List<DiscoveryBookResponse> mostViewedThisWeek) {
}
