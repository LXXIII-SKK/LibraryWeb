package com.example.library.circulation;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies/current")
class PolicyController {

    private final PolicyService policyService;

    PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadPolicies()")
    LibraryPolicyResponse currentPolicy() {
        return policyService.currentPolicy();
    }

    @PutMapping
    @PreAuthorize("@authorizationService.canManagePolicies()")
    LibraryPolicyResponse update(@Valid @RequestBody LibraryPolicyRequest request) {
        return policyService.update(request);
    }
}
