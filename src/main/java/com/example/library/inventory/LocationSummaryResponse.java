package com.example.library.inventory;

public record LocationSummaryResponse(
        Long id,
        String code,
        String name,
        String floorLabel,
        String zoneLabel,
        boolean active) {

    static LocationSummaryResponse from(LibraryLocation location) {
        if (location == null) {
            return null;
        }
        return new LocationSummaryResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getFloorLabel(),
                location.getZoneLabel(),
                location.isActive());
    }
}
