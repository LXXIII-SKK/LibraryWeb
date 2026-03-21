package com.example.library.branch;

public record LibraryBranchResponse(
        Long id,
        String code,
        String name,
        String address,
        String phone,
        boolean active) {

    public static LibraryBranchResponse from(LibraryBranch branch) {
        return new LibraryBranchResponse(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.getAddress(),
                branch.getPhone(),
                branch.isActive());
    }
}
