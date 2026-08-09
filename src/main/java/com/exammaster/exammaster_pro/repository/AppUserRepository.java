package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.AppUser;
import com.exammaster.exammaster_pro.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<AppUser> findByRoleAndEnabledOrderByCollegeNameAsc(Role role, boolean enabled);
    Optional<AppUser> findByCollegeNameIgnoreCase(String collegeName);
}
