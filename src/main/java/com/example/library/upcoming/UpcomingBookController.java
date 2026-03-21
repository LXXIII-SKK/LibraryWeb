package com.example.library.upcoming;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/upcoming-books")
class UpcomingBookController {

    private final UpcomingBookService upcomingBookService;

    UpcomingBookController(UpcomingBookService upcomingBookService) {
        this.upcomingBookService = upcomingBookService;
    }

    @GetMapping
    List<UpcomingBookResponse> listUpcomingBooks() {
        return upcomingBookService.listPublic();
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canManageCatalog()")
    UpcomingBookResponse createUpcomingBook(@Valid @RequestBody UpcomingBookRequest request) {
        return upcomingBookService.create(request);
    }

    @PutMapping("/{upcomingBookId}")
    @PreAuthorize("@authorizationService.canManageCatalog()")
    UpcomingBookResponse updateUpcomingBook(
            @PathVariable Long upcomingBookId,
            @Valid @RequestBody UpcomingBookRequest request) {
        return upcomingBookService.update(upcomingBookId, request);
    }

    @DeleteMapping("/{upcomingBookId}")
    @PreAuthorize("@authorizationService.canManageCatalog()")
    void deleteUpcomingBook(@PathVariable Long upcomingBookId) {
        upcomingBookService.delete(upcomingBookId);
    }
}
