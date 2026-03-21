package com.example.library.identity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
class ProfileController {

    private final CurrentUserService currentUserService;

    ProfileController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    ProfileResponse currentProfile() {
        return currentUserService.getCurrentProfile();
    }
}
