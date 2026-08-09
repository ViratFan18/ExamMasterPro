package com.exammaster.exammaster_pro.config;

import com.exammaster.exammaster_pro.entity.*;
import com.exammaster.exammaster_pro.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedSuperAdmin() {
        return args -> {
            if (!users.existsByUsername("superadmin")) {
                AppUser admin = new AppUser();
                admin.setCollegeName("ExamMaster Pro");
                admin.setUsername("superadmin");
                admin.setEmail("admin@exammaster.local");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ROLE_SUPER_ADMIN);
                admin.setEnabled(true);
                users.save(admin);
            }
        };
    }
}
