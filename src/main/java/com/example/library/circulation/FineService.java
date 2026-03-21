package com.example.library.circulation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.library.identity.AccessScope;
import com.example.library.identity.AppPermission;
import com.example.library.identity.AppUser;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FineService {

    private final FineRecordRepository fineRecordRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final PolicyService policyService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public FineService(
            FineRecordRepository fineRecordRepository,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            PolicyService policyService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.fineRecordRepository = fineRecordRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.policyService = policyService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<FineResponse> listForCurrentUser() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return fineRecordRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.id()).stream()
                .map(FineResponse::from)
                .toList();
    }

    public List<FineResponse> listAll() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<FineRecord> fines = switch (currentUser.scope()) {
            case GLOBAL -> fineRecordRepository.findAllByOrderByCreatedAtDesc();
            case BRANCH -> {
                if (currentUser.branchId() == null || !currentUser.hasPermission(AppPermission.FINE_READ_BRANCH)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Branch-scoped user requires fine-read permission");
                }
                yield fineRecordRepository.findAllByUser_Branch_IdOrderByCreatedAtDesc(currentUser.branchId());
            }
            case SELF -> throw new org.springframework.security.access.AccessDeniedException(
                    "Self-scoped users cannot view operational fines");
        };
        return fines.stream()
                .map(FineResponse::from)
                .toList();
    }

    @Transactional
    public FineResponse waive(Long fineId, WaiveFineRequest request) {
        FineRecord fine = fineRecordRepository.findById(fineId)
                .orElseThrow(() -> new EntityNotFoundException("Fine %d was not found".formatted(fineId)));
        LibraryPolicy policy = policyService.getCurrentPolicyEntity();
        authorizationService.assertCanWaiveFineForUser(fine.getUser(), fine.getAmount(), policy.getFineWaiverLimit());
        AppUser currentUser = currentUserService.getCurrentUserEntity();
        fine.waive(currentUser, request.note(), Instant.now());
        applicationEventPublisher.publishEvent(new FineWaivedEvent(
                currentUser.getId(),
                currentUser.getUsername(),
                fine.getBorrowTransaction() != null ? fine.getBorrowTransaction().getBook().getId() : null,
                fine.getBorrowTransaction() != null ? fine.getBorrowTransaction().getBook().getTitle() : null,
                fine.getUser().getUsername(),
                fine.getAmount(),
                fine.getResolutionNote(),
                fine.getResolvedAt()));
        return FineResponse.from(fine);
    }

    @Transactional
    public void createOverdueFineIfRequired(BorrowTransaction transaction, Instant returnedAt) {
        if (!returnedAt.isAfter(transaction.getDueAt()) || fineRecordRepository.existsByBorrowTransactionId(transaction.getId())) {
            return;
        }

        long overdueDays = Math.max(1L, ChronoUnit.DAYS.between(transaction.getDueAt(), returnedAt));
        LibraryPolicy policy = policyService.getCurrentPolicyEntity();
        BigDecimal amount = policy.getFinePerOverdueDay()
                .multiply(BigDecimal.valueOf(overdueDays))
                .setScale(2, RoundingMode.HALF_UP);
        String reason = "Returned %d day%s late".formatted(overdueDays, overdueDays == 1 ? "" : "s");

        fineRecordRepository.save(new FineRecord(transaction.getUser(), transaction, amount, reason, returnedAt));
    }
}
