package com.example.library.identity;

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
@RequestMapping("/api/users")
class AccessManagementController {

    private final AccessManagementService accessManagementService;
    private final StaffRegistrationService staffRegistrationService;

    AccessManagementController(
            AccessManagementService accessManagementService,
            StaffRegistrationService staffRegistrationService) {
        this.accessManagementService = accessManagementService;
        this.staffRegistrationService = staffRegistrationService;
    }

    @GetMapping("/options")
    @PreAuthorize("@authorizationService.canReadUsers()")
    AccessOptionsResponse options() {
        return accessManagementService.options();
    }

    @GetMapping("/staff-registration/options")
    @PreAuthorize("@authorizationService.canRegisterStaff()")
    AccessOptionsResponse staffRegistrationOptions() {
        return staffRegistrationService.options();
    }

    @PostMapping("/staff-registration")
    @PreAuthorize("@authorizationService.canRegisterStaff()")
    UserAccessResponse registerStaff(@Valid @RequestBody StaffRegistrationRequest request) {
        return staffRegistrationService.register(request);
    }

    @GetMapping("/{userId}/options")
    @PreAuthorize("@authorizationService.canReadUsers()")
    AccessOptionsResponse optionsForUser(@PathVariable Long userId) {
        return accessManagementService.optionsForUser(userId);
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadUsers()")
    List<UserAccessResponse> listUsers() {
        return accessManagementService.listUsers();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@authorizationService.canReadUsers()")
    UserAccessResponse getUser(@PathVariable Long userId) {
        return accessManagementService.getUser(userId);
    }

    @GetMapping("/{userId}/discipline")
    @PreAuthorize("@authorizationService.canReadUsers()")
    List<UserDisciplineRecordResponse> disciplineHistory(@PathVariable Long userId) {
        return accessManagementService.listUserDisciplineHistory(userId);
    }

    @PutMapping("/{userId}/access")
    @PreAuthorize("@authorizationService.canManageUsers()")
    UserAccessResponse updateUserAccess(@PathVariable Long userId, @Valid @RequestBody UserAccessUpdateRequest request) {
        return accessManagementService.updateUserAccess(userId, request);
    }

    @PostMapping("/{userId}/discipline")
    @PreAuthorize("@authorizationService.canManageUserDiscipline()")
    UserDisciplineRecordResponse applyDiscipline(
            @PathVariable Long userId,
            @Valid @RequestBody UserDisciplineRequest request) {
        return accessManagementService.applyUserDiscipline(userId, request);
    }
}
