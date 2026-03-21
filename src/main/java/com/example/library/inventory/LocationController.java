package com.example.library.inventory;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
class LocationController {

    private final LocationService locationService;

    LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canManageInventory()")
    List<LibraryLocationResponse> listLocations() {
        return locationService.listManagedLocations();
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canManageInventory()")
    LibraryLocationResponse createLocation(@Valid @RequestBody LocationUpsertRequest request) {
        return locationService.create(request);
    }

    @PutMapping("/{locationId}")
    @PreAuthorize("@authorizationService.canManageInventory()")
    LibraryLocationResponse updateLocation(@PathVariable Long locationId, @Valid @RequestBody LocationUpsertRequest request) {
        return locationService.update(locationId, request);
    }
}
