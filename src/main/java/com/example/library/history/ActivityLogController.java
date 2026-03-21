package com.example.library.history;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity-logs")
class ActivityLogController {

    private final ActivityLogService activityLogService;

    ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/me")
    @PreAuthorize("@authorizationService.canReadOwnHistory()")
    List<ActivityLogResponse> myActivityLogs() {
        return activityLogService.listForCurrentUser();
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadAuditLogs()")
    List<ActivityLogResponse> allActivityLogs() {
        return activityLogService.listAll();
    }
}
