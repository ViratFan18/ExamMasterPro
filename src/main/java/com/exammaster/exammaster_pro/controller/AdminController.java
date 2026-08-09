package com.exammaster.exammaster_pro.controller;

import com.exammaster.exammaster_pro.dto.*;
import com.exammaster.exammaster_pro.dto.Requests.*;
import com.exammaster.exammaster_pro.dto.Responses.*;
import com.exammaster.exammaster_pro.entity.*;
import com.exammaster.exammaster_pro.exception.BusinessValidationException;
import com.exammaster.exammaster_pro.exception.ResourceNotFoundException;
import com.exammaster.exammaster_pro.repository.*;
import com.exammaster.exammaster_pro.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AppUserRepository users;
    private final BuildingRepository buildings;
    private final HallRepository halls;
    private final StudentRepository students;
    private final InvigilatorRepository invigilators;
    private final ExamRepository exams;
    private final AllocationRepository allocations;
    private final ComplaintRepository complaints;
    private final AuthService auth;
    private final PasswordEncoder passwordEncoder;
    private final ExamMasterService app;
    private final CurrentUserService current;

    @GetMapping("/users")
    ApiResponse<?> users() {
        return ApiResponse.ok("Users loaded successfully.", users.findAll().stream().map(this::userResponse).toList());
    }

    @PostMapping("/users")
    ApiResponse<?> createUser(@Valid @RequestBody UserRequest request) {
        return ApiResponse.ok("User created successfully.", userResponse(auth.createUser(request)));
    }

    @PostMapping("/users/{id}/disable")
    ApiResponse<?> disable(@PathVariable Long id) {
        AppUser user = findUser(id);
        user.setEnabled(false);
        users.save(user);
        return ApiResponse.ok("User disabled successfully.", userResponse(user));
    }

    @PostMapping("/users/{id}/enable")
    ApiResponse<?> enable(@PathVariable Long id) {
        AppUser user = findUser(id);
        user.setEnabled(true);
        users.save(user);
        return ApiResponse.ok("User enabled successfully.", userResponse(user));
    }

    @PostMapping("/users/{id}/reset-password")
    ApiResponse<?> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        AppUser user = findUser(id);
        user.setPassword(passwordEncoder.encode(request.password()));
        users.save(user);
        return ApiResponse.ok("Password reset successfully.", null);
    }

    @DeleteMapping("/users/{id}")
    ApiResponse<?> deleteUser(@PathVariable Long id) {
        users.delete(findUser(id));
        return ApiResponse.ok("User deleted successfully.", null);
    }

    @PostMapping("/users/{id}/clear/{type}")
    ApiResponse<?> clearUserData(@PathVariable Long id, @PathVariable String type) {
        AppUser user = findUser(id);
        switch (type) {
            case "allocations" -> app.deleteAllAllocations(user);
            case "buildings" -> app.deleteAllBuildings(user);
            case "halls" -> app.deleteAllHalls(user);
            case "students" -> app.deleteAllStudents(user);
            case "invigilators" -> app.deleteAllInvigilators(user);
            case "exams" -> app.deleteAllExams(user);
            default -> throw new BusinessValidationException("Invalid clear action. Use allocations, buildings, halls, students, invigilators, or exams.");
        }
        return ApiResponse.ok("User " + type + " cleared successfully.", null);
    }

    @PatchMapping("/complaints/{id}")
    ApiResponse<?> updateComplaint(@PathVariable Long id, @Valid @RequestBody ComplaintStatusRequest request) {
        return ApiResponse.ok("Complaint updated successfully.", app.updateComplaint(id, request.status(), current.currentUser().getUsername()));
    }

    @GetMapping("/complaints")
    ApiResponse<?> complaints() {
        return ApiResponse.ok("Complaints loaded successfully.", complaints.findAllByOrderByCreatedAtDesc().stream()
                .map(this::adminComplaintResponse)
                .toList());
    }

    @GetMapping("/storage")
    ApiResponse<?> storage() {
        return ApiResponse.ok("Storage analytics loaded successfully.", users.findAll().stream()
                .filter(user -> user.getRole() == Role.ROLE_USER)
                .map(user -> new StorageAnalytics(user.getId(), user.getCollegeName(), buildings.countByUser(user), halls.countByUser(user),
                        students.countByUser(user), invigilators.countByUser(user), exams.countByUser(user), allocations.countByUser(user),
                        buildings.countByUser(user) + halls.countByUser(user) + students.countByUser(user) + invigilators.countByUser(user) + exams.countByUser(user) + allocations.countByUser(user)))
                .toList());
    }

    private AppUser findUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }

    private UserResponse userResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getCollegeName(), user.getUsername(), user.getEmail(), user.getRole(), user.isEnabled());
    }

    private AdminComplaintResponse adminComplaintResponse(Complaint complaint) {
        AppUser user = complaint.getUser();
        return new AdminComplaintResponse(complaint.getId(), user.getId(), user.getCollegeName(), complaint.getTitle(),
                complaint.getDescription(), complaint.getCategory(), complaint.getEmail(), complaint.getStatus(), complaint.getCreatedAt());
    }
}
