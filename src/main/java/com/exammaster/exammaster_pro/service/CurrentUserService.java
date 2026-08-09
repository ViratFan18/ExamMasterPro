package com.exammaster.exammaster_pro.service;

import com.exammaster.exammaster_pro.entity.AppUser;
import com.exammaster.exammaster_pro.entity.Role;
import com.exammaster.exammaster_pro.exception.ResourceNotFoundException;
import com.exammaster.exammaster_pro.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final AppUserRepository users;
    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);

    public AppUser currentUser() {
        log.info("Resolving current authenticated user from security context.");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Please log in to continue.");
        }
        return users.findByUsername(auth.getName()).orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
    }

    public AppUser workspaceUser(Long userId) {
        AppUser current = currentUser();
        log.info("Selecting workspace user. requestedUserId={}, currentUser={}", userId, current == null ? "anonymous" : current.getUsername());
        if (current.getRole() == Role.ROLE_SUPER_ADMIN && userId != null) {
            return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User workspace was not found."));
        }
        return current;
    }
}
