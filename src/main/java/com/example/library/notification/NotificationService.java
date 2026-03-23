package com.example.library.notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.common.OperationalActivityEvent;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
import com.example.library.identity.AppUserRepository;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final Set<AppRole> ALLOWED_TARGET_ROLES = Set.of(
            AppRole.MEMBER,
            AppRole.LIBRARIAN,
            AppRole.BRANCH_MANAGER,
            AppRole.ADMIN,
            AppRole.AUDITOR);

    private final StaffNotificationRepository staffNotificationRepository;
    private final StaffNotificationReceiptRepository staffNotificationReceiptRepository;
    private final BranchService branchService;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public NotificationService(
            StaffNotificationRepository staffNotificationRepository,
            StaffNotificationReceiptRepository staffNotificationReceiptRepository,
            BranchService branchService,
            AppUserRepository appUserRepository,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.staffNotificationRepository = staffNotificationRepository;
        this.staffNotificationReceiptRepository = staffNotificationReceiptRepository;
        this.branchService = branchService;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<StaffNotificationResponse> listCurrentNotifications() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!authorizationService.canReadStaffNotifications()) {
            throw new AccessDeniedException("This role cannot read notifications");
        }

        Map<Long, StaffNotificationReceipt> receiptsByNotificationId = staffNotificationReceiptRepository.findAllByUser_Id(currentUser.id()).stream()
                .collect(Collectors.toMap(
                        receipt -> receipt.getNotification().getId(),
                        Function.identity()));

        return staffNotificationRepository.findVisibleToUserOrderByCreatedAtDesc(
                        currentUser.id(),
                        currentUser.role(),
                        currentUser.branchId())
                .stream()
                .map(notification -> StaffNotificationResponse.from(
                        notification,
                        receiptsByNotificationId.get(notification.getId())))
                .toList();
    }

    @Transactional
    public StaffNotificationResponse create(CreateStaffNotificationRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!authorizationService.canSendStaffNotifications()) {
            throw new AccessDeniedException("This role cannot send staff notifications");
        }
        if (!ALLOWED_TARGET_ROLES.containsAll(request.targetRoles())) {
            throw new IllegalArgumentException("Notifications can only target recognized library roles");
        }

        LibraryBranch branch = resolveRequestedBranch(request.branchId(), currentUser);
        AppUser creator = currentUserService.getCurrentUserEntity();
        StaffNotification notification = staffNotificationRepository.save(new StaffNotification(
                request.title(),
                request.message(),
                creator,
                null,
                branch,
                request.targetRoles()));
        applicationEventPublisher.publishEvent(new OperationalActivityEvent(
                currentUser.id(),
                "NOTIFICATION_CREATED",
                "%s created notification [%s] for roles=%s, branch=%s".formatted(
                        currentUser.username(),
                        notification.getTitle(),
                        notification.getTargetRoles(),
                        describeBranch(branch)),
                notification.getCreatedAt()));
        return StaffNotificationResponse.from(notification, null);
    }

    @Transactional
    public DisciplineRequestNotificationResponse createDisciplineRequest(CreateDisciplineRequestNotificationRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!authorizationService.canRequestUserDiscipline()) {
            throw new AccessDeniedException("This role cannot request user discipline");
        }
        if (currentUser.branchId() == null) {
            throw new AccessDeniedException("Branch-scoped staff require a branch assignment");
        }

        AppUser targetUser = appUserRepository.findByUsername(request.targetUsername().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User %s was not found".formatted(request.targetUsername().trim())));
        if (targetUser.getRole() != AppRole.MEMBER) {
            throw new AccessDeniedException("Librarians can only request discipline review for member accounts");
        }
        if (!currentUser.belongsToBranch(targetUser.getBranchId())) {
            throw new AccessDeniedException("This user is outside your branch");
        }

        LibraryBranch branch = branchService.resolveBranch(currentUser.branchId());
        AppUser creator = currentUserService.getCurrentUserEntity();
        String normalizedNote = normalizeOptional(request.note());
        String title = "Discipline request for %s".formatted(targetUser.getUsername());
        String message = disciplineRequestMessage(
                creator.getUsername(),
                targetUser.getUsername(),
                branch.getName(),
                request.action(),
                request.reason(),
                normalizedNote);

        List<AppUser> recipients = disciplineRequestRecipients(branch.getId());
        if (recipients.isEmpty()) {
            throw new IllegalStateException("No branch manager or administrator is available to review this request");
        }

        Instant createdAt = Instant.now();
        for (AppUser recipient : recipients) {
            StaffNotification saved = staffNotificationRepository.save(new StaffNotification(
                    title,
                    message,
                    creator,
                    recipient,
                    branch,
                    Set.of(recipient.getRole())));
            createdAt = saved.getCreatedAt();
        }
        applicationEventPublisher.publishEvent(new OperationalActivityEvent(
                currentUser.id(),
                "NOTIFICATION_CREATED",
                "%s created discipline-review notifications for %s, notifying %s".formatted(
                        currentUser.username(),
                        targetUser.getUsername(),
                        recipients.stream().map(AppUser::getUsername).toList()),
                createdAt));

        return new DisciplineRequestNotificationResponse(
                targetUser.getUsername(),
                request.action(),
                request.reason(),
                normalizedNote,
                recipients.stream().map(AppUser::getUsername).toList(),
                createdAt);
    }

    @Transactional
    public StaffNotificationResponse markRead(Long notificationId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!authorizationService.canReadStaffNotifications()) {
            throw new AccessDeniedException("This role cannot read staff notifications");
        }
        StaffNotification notification = staffNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification %d was not found".formatted(notificationId)));
        if (!isVisibleTo(notification, currentUser)) {
            throw new AccessDeniedException("This notification is outside your scope");
        }

        AppUser user = currentUserService.getCurrentUserEntity();
        StaffNotificationReceipt existingReceipt = staffNotificationReceiptRepository.findByNotification_IdAndUser_Id(notificationId, user.getId())
                .orElse(null);
        StaffNotificationReceipt receipt = existingReceipt != null
                ? existingReceipt
                : staffNotificationReceiptRepository.save(new StaffNotificationReceipt(notification, user, Instant.now()));
        if (existingReceipt == null) {
            applicationEventPublisher.publishEvent(new OperationalActivityEvent(
                    currentUser.id(),
                    "NOTIFICATION_READ",
                    "%s marked notification %d as read [%s]".formatted(
                            currentUser.username(),
                            notification.getId(),
                            notification.getTitle()),
                    receipt.getReadAt()));
        }
        return StaffNotificationResponse.from(notification, receipt);
    }

    private boolean isVisibleTo(StaffNotification notification, CurrentUser currentUser) {
        if (notification.getTargetUser() != null) {
            return notification.getTargetUser().getId() != null
                    && notification.getTargetUser().getId().equals(currentUser.id());
        }
        if (!notification.getTargetRoles().contains(currentUser.role())) {
            return false;
        }
        if (notification.getBranch() == null) {
            return true;
        }
        return currentUser.branchId() != null && currentUser.branchId().equals(notification.getBranch().getId());
    }

    private LibraryBranch resolveRequestedBranch(Long branchId, CurrentUser currentUser) {
        if (currentUser.role() == AppRole.ADMIN) {
            return branchService.findBranchOrNull(branchId);
        }
        if (currentUser.branchId() == null) {
            throw new AccessDeniedException("Branch-scoped staff require a branch assignment");
        }
        if (branchId != null && !currentUser.branchId().equals(branchId)) {
            throw new AccessDeniedException("Branch-scoped staff can only send notifications for their branch");
        }
        return branchService.resolveBranch(currentUser.branchId());
    }

    @Transactional
    public StaffNotificationResponse notifyUser(
            AppUser targetUser,
            String title,
            String message,
            LibraryBranch branch,
            AppUser creator) {
        StaffNotification notification = staffNotificationRepository.save(new StaffNotification(
                title,
                message,
                creator,
                targetUser,
                branch,
                Set.of(targetUser.getRole())));
        return StaffNotificationResponse.from(notification, null);
    }

    private List<AppUser> disciplineRequestRecipients(Long branchId) {
        Map<Long, AppUser> recipients = new LinkedHashMap<>();
        appUserRepository.findAllByBranch_IdAndRoleOrderByUsernameAsc(branchId, AppRole.BRANCH_MANAGER)
                .forEach(user -> recipients.put(user.getId(), user));
        appUserRepository.findAllByRoleOrderByUsernameAsc(AppRole.ADMIN)
                .forEach(user -> recipients.put(user.getId(), user));
        return List.copyOf(recipients.values());
    }

    private String disciplineRequestMessage(
            String requesterUsername,
            String targetUsername,
            String branchName,
            com.example.library.identity.UserDisciplineActionType action,
            com.example.library.identity.UserDisciplineReason reason,
            String note) {
        String base = "%s requested %s review for member %s at %s. Reason: %s."
                .formatted(requesterUsername, action, targetUsername, branchName, reason);
        return note == null ? base : "%s Note: %s".formatted(base, note);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String describeBranch(LibraryBranch branch) {
        if (branch == null) {
            return "global";
        }
        return "%s (%s)".formatted(branch.getName(), branch.getCode());
    }
}
