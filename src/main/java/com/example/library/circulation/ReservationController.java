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
@RequestMapping("/api/reservations")
class ReservationController {

    private final ReservationService reservationService;

    ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canCreateReservationForSelf()")
    ReservationResponse create(@Valid @RequestBody CreateReservationRequest request) {
        return reservationService.createForCurrentUser(request);
    }

    @GetMapping("/me")
    @PreAuthorize("@authorizationService.canReadOwnReservations()")
    List<ReservationResponse> myReservations() {
        return reservationService.listForCurrentUser();
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadOperationalReservations()")
    List<ReservationResponse> allReservations() {
        return reservationService.listAll();
    }

    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("@authorizationService.canReadOwnReservations() or @authorizationService.canManageReservations()")
    ReservationResponse cancel(@PathVariable Long reservationId) {
        return reservationService.cancel(reservationId);
    }

    @PostMapping("/{reservationId}/prepare")
    @PreAuthorize("@authorizationService.canManageReservations()")
    ReservationResponse prepare(@PathVariable Long reservationId, @RequestBody(required = false) PrepareReservationRequest request) {
        return reservationService.prepareForPickup(reservationId, request);
    }

    @PostMapping("/{reservationId}/ready")
    @PreAuthorize("@authorizationService.canManageReservations()")
    ReservationResponse markReady(@PathVariable Long reservationId) {
        return reservationService.markReadyForPickup(reservationId);
    }

    @PostMapping("/{reservationId}/expire")
    @PreAuthorize("@authorizationService.canManageReservations()")
    ReservationResponse expire(@PathVariable Long reservationId) {
        return reservationService.expire(reservationId);
    }

    @PostMapping("/{reservationId}/collect")
    @PreAuthorize("@authorizationService.canBorrowForSelf()")
    BorrowTransactionResponse collect(@PathVariable Long reservationId) {
        return reservationService.collectForCurrentUser(reservationId);
    }

    @PostMapping("/{reservationId}/no-show")
    @PreAuthorize("@authorizationService.canManageReservations()")
    ReservationResponse noShow(@PathVariable Long reservationId) {
        return reservationService.markNoShow(reservationId);
    }
}
