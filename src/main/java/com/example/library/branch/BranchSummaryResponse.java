package com.example.library.branch;

public record BranchSummaryResponse(
        Long id,
        String code,
        String name,
        boolean active) {

    public static BranchSummaryResponse from(LibraryBranch branch) {
        if (branch == null) {
            return null;
        }
        return new BranchSummaryResponse(branch.getId(), branch.getCode(), branch.getName(), branch.isActive());
    }
}
