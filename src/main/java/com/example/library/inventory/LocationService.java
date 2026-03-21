package com.example.library.inventory;

import java.util.List;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final LibraryLocationRepository libraryLocationRepository;
    private final BranchService branchService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;

    public LocationService(
            LibraryLocationRepository libraryLocationRepository,
            BranchService branchService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService) {
        this.libraryLocationRepository = libraryLocationRepository;
        this.branchService = branchService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
    }

    public List<LibraryLocationResponse> listManagedLocations() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<LibraryLocation> locations = switch (currentUser.scope()) {
            case GLOBAL -> libraryLocationRepository.findAllByOrderByBranch_NameAscNameAsc();
            case BRANCH -> {
                if (!authorizationService.canManageInventory() || currentUser.branchId() == null) {
                    throw new AccessDeniedException("Branch-scoped staff require inventory permissions");
                }
                yield libraryLocationRepository.findAllByBranch_IdOrderByNameAsc(currentUser.branchId());
            }
            case SELF -> throw new AccessDeniedException("This role cannot manage library locations");
        };
        return locations.stream()
                .map(LibraryLocationResponse::from)
                .toList();
    }

    public LibraryLocation findEntity(Long locationId) {
        return libraryLocationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Location %d was not found".formatted(locationId)));
    }

    @Transactional
    public LibraryLocationResponse create(LocationUpsertRequest request) {
        LibraryBranch branch = branchService.resolveBranch(request.branchId());
        authorizationService.assertCanManageBranchInventory(branch.getId());
        assertUniqueCode(branch.getId(), request.code(), null);
        LibraryLocation location = libraryLocationRepository.save(new LibraryLocation(
                branch,
                request.code(),
                request.name(),
                request.floorLabel(),
                request.zoneLabel(),
                request.active()));
        return LibraryLocationResponse.from(location);
    }

    @Transactional
    public LibraryLocationResponse update(Long locationId, LocationUpsertRequest request) {
        LibraryLocation location = findEntity(locationId);
        LibraryBranch branch = branchService.resolveBranch(request.branchId());
        authorizationService.assertCanManageBranchInventory(branch.getId());
        if (location.getBranch() != null && location.getBranch().getId() != null) {
            authorizationService.assertCanManageBranchInventory(location.getBranch().getId());
        }
        assertUniqueCode(branch.getId(), request.code(), locationId);
        location.updateDetails(
                branch,
                request.code(),
                request.name(),
                request.floorLabel(),
                request.zoneLabel(),
                request.active());
        return LibraryLocationResponse.from(location);
    }

    private void assertUniqueCode(Long branchId, String code, Long locationId) {
        boolean exists = locationId == null
                ? libraryLocationRepository.existsByBranch_IdAndCodeIgnoreCase(branchId, code)
                : libraryLocationRepository.existsByBranch_IdAndCodeIgnoreCaseAndIdNot(branchId, code, locationId);
        if (exists) {
            throw new IllegalArgumentException("Location code is already in use for this branch");
        }
    }
}
