package com.example.library.identity;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByKeycloakUserId(String keycloakUserId);

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findAllByOrderByUsernameAsc();

    List<AppUser> findAllByRoleOrderByUsernameAsc(AppRole role);

    List<AppUser> findAllByBranch_IdOrderByUsernameAsc(Long branchId);

    List<AppUser> findAllByBranch_IdAndRoleOrderByUsernameAsc(Long branchId, AppRole role);

    List<AppUser> findAllByBranch_IdAndRoleInOrderByUsernameAsc(Long branchId, List<AppRole> roles);
}
