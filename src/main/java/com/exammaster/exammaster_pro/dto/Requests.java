package com.exammaster.exammaster_pro.dto;

import com.exammaster.exammaster_pro.entity.ComplaintStatus;
import com.exammaster.exammaster_pro.entity.ExamType;
import jakarta.validation.constraints.*;

public final class Requests {
    private Requests() {}

    public record RegisterRequest(@NotBlank(message = "College name is required") String collegeName,
                                  @NotBlank(message = "Username is required") String username,
                                  @Email(message = "Email must be a valid email address") @NotBlank(message = "Email is required") String email,
                                  @Size(min = 6, message = "Password must contain at least 6 characters") String password,
                                  @NotBlank(message = "Confirm password is required") String confirmPassword) {}

    public record LoginRequest(@NotBlank(message = "Username is required") String username,
                               @NotBlank(message = "Password is required") String password) {}

    public record UserRequest(@NotBlank(message = "College name is required") String collegeName,
                              @NotBlank(message = "Username is required") String username,
                              @Email(message = "Email must be a valid email address") @NotBlank(message = "Email is required") String email,
                              @Size(min = 6, message = "Password must contain at least 6 characters") String password) {}

    public record ResetPasswordRequest(@Size(min = 6, message = "Password must contain at least 6 characters") String password) {}

    public record BuildingRequest(@NotBlank(message = "Building name is required") @Size(min = 2, max = 100, message = "Building name must be 2-100 characters") String buildingName,
                                  @Positive(message = "Maximum halls must be a positive number") @Max(value = 100, message = "Maximum halls cannot exceed 100") int maxHallCount) {}

    public record HallRequest(@NotNull(message = "Building must be selected") Long buildingId,
                              @NotBlank(message = "Hall name is required") @Pattern(regexp = "^[A-Za-z0-9._-]{1,50}$", message = "Hall name must be 1-50 characters using letters, numbers, dot, underscore, or hyphen; spaces are not allowed") String hallName,
                              @Positive(message = "Bench count must be a positive whole number") @Max(value = 500, message = "Bench count cannot exceed 500") int benchCount,
                              @Min(value = 1, message = "Students per bench must be 1 or 2") @Max(value = 2, message = "Students per bench must be 1 or 2") int studentsPerBench) {}

    public record StudentRequest(@NotBlank(message = "Hall ticket number is required") @Pattern(regexp = "^[A-Z0-9]{3,20}$", message = "Hall ticket must be 3-20 uppercase letters or digits") String hallTicketNumber,
                                 @NotBlank(message = "Student name is required") @Size(min = 2, max = 100, message = "Student name must be 2-100 characters") @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Student name can contain only letters, spaces, dots, hyphens, and apostrophes") String studentName,
                                 @NotBlank(message = "Branch is required") @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Branch must be 2-10 uppercase letters or digits") String branch,
                                 @NotBlank(message = "Year is required") @Pattern(regexp = "^[1-4]$", message = "Year must be 1, 2, 3, or 4") String year,
                                 @NotBlank(message = "Semester is required") @Pattern(regexp = "^[1-8]$", message = "Semester must be 1-8") String semester,
                                 @NotBlank(message = "Section is required") @Pattern(regexp = "^[A-Z]$", message = "Section must be a single uppercase letter") String section) {}

    public record InvigilatorRequest(@NotBlank(message = "Invigilator ID is required") @Pattern(regexp = "^[A-Z0-9]{2,20}$", message = "Invigilator ID must be 2-20 uppercase letters or digits") String invigilatorId,
                                     @NotBlank(message = "Invigilator name is required") @Size(min = 2, max = 100, message = "Invigilator name must be 2-100 characters") String invigilatorName) {}

    public record ExamRequest(@NotBlank(message = "Exam name is required") @Size(min = 2, max = 100, message = "Exam name must be 2-100 characters") String examName,
                              @NotBlank(message = "Academic year is required") @Pattern(regexp = "^[0-9]{4}-[0-9]{4}$", message = "Academic year must be in format YYYY-YYYY") String academicYear,
                              @NotBlank(message = "Semester is required") @Pattern(regexp = "^[1-8]$", message = "Semester must be 1-8") String semester,
                              @NotNull(message = "Exam type is required") ExamType examType) {}

    public record ComplaintRequest(@NotBlank(message = "Title is required") @Size(min = 2, max = 200, message = "Title must be 2-200 characters") String title,
                                   @NotBlank(message = "Description is required") @Size(min = 5, max = 3000, message = "Description must be 5-3000 characters") String description,
                                   @NotBlank(message = "Category is required") @Pattern(regexp = "^[A-Z_ ]{2,30}$", message = "Category must be uppercase letters, spaces or underscores (2-30 chars)") String category,
                                   @Email(message = "Email must be a valid email address") @NotBlank(message = "Email is required") String email) {}

    public record ComplaintStatusRequest(@NotNull(message = "Complaint status is required") ComplaintStatus status) {}
}
