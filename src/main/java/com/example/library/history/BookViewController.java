package com.example.library.history;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BookViewController {

    private final ActivityLogService activityLogService;

    BookViewController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @PostMapping("/api/books/{id}/view")
    @PreAuthorize("@authorizationService.canRecordBookView()")
    BookViewRecordResponse recordView(@PathVariable Long id) {
        return activityLogService.recordView(id);
    }
}
