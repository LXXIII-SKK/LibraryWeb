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
@RequestMapping("/api/borrowings")
class BorrowController {

    private final CirculationService circulationService;

    BorrowController(CirculationService circulationService) {
        this.circulationService = circulationService;
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canBorrowForSelf()")
    BorrowTransactionResponse borrow(@Valid @RequestBody BorrowBookRequest request) {
        return circulationService.borrow(request);
    }

    @PostMapping("/staff-checkout")
    @PreAuthorize("@authorizationService.canCheckoutForMember()")
    BorrowTransactionResponse staffCheckout(@Valid @RequestBody StaffCheckoutRequest request) {
        return circulationService.staffCheckout(request);
    }

    @PostMapping("/{transactionId}/return")
    @PreAuthorize("@authorizationService.canAccessReturnEndpoint()")
    BorrowTransactionResponse returnBook(@PathVariable Long transactionId) {
        return circulationService.returnBook(transactionId);
    }

    @PostMapping("/{transactionId}/renew")
    @PreAuthorize("@authorizationService.canRenewBorrowings()")
    BorrowTransactionResponse renew(
            @PathVariable Long transactionId,
            @RequestBody(required = false) RenewBorrowingRequest request) {
        return circulationService.renewBorrowing(transactionId, request);
    }

    @GetMapping("/me")
    @PreAuthorize("@authorizationService.canReadOwnBorrowings()")
    List<BorrowTransactionResponse> myBorrowings() {
        return circulationService.listForCurrentUser();
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadOperationalBorrowings()")
    List<BorrowTransactionResponse> allBorrowings() {
        return circulationService.listAll();
    }
}
