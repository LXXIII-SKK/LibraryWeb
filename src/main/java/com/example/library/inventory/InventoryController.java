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
@RequestMapping("/api/inventory")
class InventoryController {

    private final InventoryService inventoryService;

    InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/holdings")
    @PreAuthorize("@authorizationService.canManageInventory()")
    List<BookHoldingResponse> listHoldings() {
        return inventoryService.listManagedHoldings();
    }

    @PostMapping("/holdings")
    @PreAuthorize("@authorizationService.canManageInventory()")
    BookHoldingResponse createHolding(@Valid @RequestBody BookHoldingUpsertRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/holdings/{holdingId}")
    @PreAuthorize("@authorizationService.canManageInventory()")
    BookHoldingResponse updateHolding(@PathVariable Long holdingId, @Valid @RequestBody BookHoldingUpsertRequest request) {
        return inventoryService.update(holdingId, request);
    }

    @GetMapping("/digital-access/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    DigitalAccessResponse digitalAccess(@PathVariable Long transactionId) {
        return inventoryService.digitalAccessForTransaction(transactionId);
    }
}
