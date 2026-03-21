package com.example.library.history;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
class BookViewController {

    private final ActivityLogService activityLogService;

    BookViewController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @PostMapping("/api/books/{id}/view")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("@authorizationService.canRecordBookView()")
    void recordView(@PathVariable Long id) {
        activityLogService.recordView(id);
    }
}
