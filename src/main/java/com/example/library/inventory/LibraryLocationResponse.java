package com.example.library.inventory;

import com.example.library.branch.BranchSummaryResponse;

public record LibraryLocationResponse(
        Long id,
        String code,
        String name,
        String floorLabel,
        String zoneLabel,
        boolean active,
        BranchSummaryResponse branch) {

    static LibraryLocationResponse from(LibraryLocation location) {
        return new LibraryLocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getFloorLabel(),
                location.getZoneLabel(),
                location.isActive(),
                BranchSummaryResponse.from(location.getBranch()));
    }
}
