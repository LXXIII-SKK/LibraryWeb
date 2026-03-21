package com.example.library.notification;

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
@RequestMapping("/api/notifications")
class NotificationController {

    private final NotificationService notificationService;

    NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadStaffNotifications()")
    List<StaffNotificationResponse> currentNotifications() {
        return notificationService.listCurrentNotifications();
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canSendStaffNotifications()")
    StaffNotificationResponse createNotification(@Valid @RequestBody CreateStaffNotificationRequest request) {
        return notificationService.create(request);
    }

    @PostMapping("/discipline-requests")
    @PreAuthorize("@authorizationService.canRequestUserDiscipline()")
    DisciplineRequestNotificationResponse createDisciplineRequest(
            @Valid @RequestBody CreateDisciplineRequestNotificationRequest request) {
        return notificationService.createDisciplineRequest(request);
    }

    @PostMapping("/{notificationId}/read")
    @PreAuthorize("@authorizationService.canReadStaffNotifications()")
    StaffNotificationResponse markRead(@PathVariable Long notificationId) {
        return notificationService.markRead(notificationId);
    }
}
