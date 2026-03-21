package com.example.library.branch;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BranchService {

    private final LibraryBranchRepository libraryBranchRepository;

    public BranchService(LibraryBranchRepository libraryBranchRepository) {
        this.libraryBranchRepository = libraryBranchRepository;
    }

    public List<LibraryBranchResponse> listAll() {
        return libraryBranchRepository.findAllByOrderByNameAsc().stream()
                .map(LibraryBranchResponse::from)
                .toList();
    }

    public List<LibraryBranchResponse> listPublicActive() {
        return libraryBranchRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(LibraryBranchResponse::from)
                .toList();
    }

    public List<BranchSummaryResponse> listSummaries() {
        return libraryBranchRepository.findAllByOrderByNameAsc().stream()
                .map(BranchSummaryResponse::from)
                .toList();
    }

    public List<BranchSummaryResponse> listSummariesByIds(Collection<Long> branchIds) {
        List<Long> ids = branchIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return libraryBranchRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(LibraryBranch::getName))
                .map(BranchSummaryResponse::from)
                .toList();
    }

    public LibraryBranch resolveBranch(Long branchId) {
        if (branchId == null) {
            return null;
        }
        return libraryBranchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch %d was not found".formatted(branchId)));
    }

    public LibraryBranch findBranchOrNull(Long branchId) {
        if (branchId == null) {
            return null;
        }
        return libraryBranchRepository.findById(branchId).orElse(null);
    }

    @Transactional
    public LibraryBranchResponse create(BranchUpsertRequest request) {
        assertUniqueCode(request.code(), null);
        LibraryBranch branch = libraryBranchRepository.save(new LibraryBranch(
                request.code(),
                request.name(),
                request.address(),
                request.phone(),
                request.active()));
        return LibraryBranchResponse.from(branch);
    }

    @Transactional
    public LibraryBranchResponse update(Long branchId, BranchUpsertRequest request) {
        LibraryBranch branch = resolveBranch(branchId);
        assertUniqueCode(request.code(), branchId);
        branch.updateDetails(
                request.code(),
                request.name(),
                request.address(),
                request.phone(),
                request.active());
        return LibraryBranchResponse.from(branch);
    }

    private void assertUniqueCode(String code, Long branchId) {
        boolean exists = branchId == null
                ? libraryBranchRepository.existsByCodeIgnoreCase(code)
                : libraryBranchRepository.existsByCodeIgnoreCaseAndIdNot(code, branchId);
        if (exists) {
            throw new IllegalArgumentException("Branch code is already in use");
        }
    }
}
