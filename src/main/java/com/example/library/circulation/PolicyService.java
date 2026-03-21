package com.example.library.circulation;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PolicyService {

    private static final long CURRENT_POLICY_ID = 1L;

    private final LibraryPolicyRepository libraryPolicyRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PolicyService(
            LibraryPolicyRepository libraryPolicyRepository,
            CurrentUserService currentUserService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.libraryPolicyRepository = libraryPolicyRepository;
        this.currentUserService = currentUserService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public LibraryPolicyResponse currentPolicy() {
        return LibraryPolicyResponse.from(getCurrentPolicyEntity());
    }

    public LibraryPolicy getCurrentPolicyEntity() {
        return libraryPolicyRepository.findById(CURRENT_POLICY_ID)
                .orElseThrow(() -> new EntityNotFoundException("Library policy is not configured"));
    }

    @Transactional
    public LibraryPolicyResponse update(LibraryPolicyRequest request) {
        LibraryPolicy policy = libraryPolicyRepository.findById(CURRENT_POLICY_ID)
                .orElseGet(() -> libraryPolicyRepository.save(new LibraryPolicy(
                        CURRENT_POLICY_ID,
                        14,
                        7,
                        2,
                        BigDecimal.valueOf(1.50),
                        BigDecimal.valueOf(10.00),
                        false)));
        policy.updateFrom(request);
        CurrentUser currentUser = currentUserService.getCurrentUser();
        applicationEventPublisher.publishEvent(new PolicyUpdatedEvent(
                currentUser.id(),
                currentUser.username(),
                Instant.now()));
        return LibraryPolicyResponse.from(policy);
    }
}
