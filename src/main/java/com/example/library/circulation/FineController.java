package com.example.library.circulation;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fines")
class FineController {

    private final FineService fineService;

    FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @GetMapping("/me")
    @PreAuthorize("@authorizationService.canReadOwnFines()")
    List<FineResponse> myFines() {
        return fineService.listForCurrentUser();
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadOperationalFines()")
    List<FineResponse> allFines() {
        return fineService.listAll();
    }

    @PostMapping("/{fineId}/waive")
    @PreAuthorize("@authorizationService.canWaiveFine()")
    FineResponse waive(@PathVariable Long fineId, @Valid @RequestBody WaiveFineRequest request) {
        return fineService.waive(fineId, request);
    }
}
