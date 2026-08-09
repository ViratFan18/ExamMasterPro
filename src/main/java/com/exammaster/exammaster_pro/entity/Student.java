package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "students", 
    uniqueConstraints = @UniqueConstraint(name = "uk_student_user_ticket", columnNames = {"user_id", "hall_ticket_number"}),
    indexes = {
        @Index(name = "idx_student_branch", columnList = "user_id, branch"),
        @Index(name = "idx_student_section", columnList = "user_id, section")
    }
)
public class Student extends OwnedEntity {
    
    @NotBlank(message = "Hall ticket number is required")
    @Pattern(regexp = "^[A-Z0-9]{3,20}$", message = "Invalid hall ticket format (3-20 alphanumeric, uppercase)")
    @Column(name = "hall_ticket_number", nullable = false, length = 20)
    private String hallTicketNumber;
    
    @NotBlank(message = "Student name is required")
    @Size(min = 2, max = 100, message = "Student name must be 2-100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Student name can only contain letters, spaces, dots, hyphens, and apostrophes")
    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;
    
    @NotBlank(message = "Branch is required")
    @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Invalid branch format (2-10 alphanumeric, uppercase)")
    @Column(nullable = false, length = 10)
    private String branch;
    
    @NotBlank(message = "Year is required")
    @Pattern(regexp = "^[1-4]$", message = "Year must be 1, 2, 3, or 4")
    @Column(name = "student_year", nullable = false, length = 1)
    private String year;
    
    @NotBlank(message = "Semester is required")
    @Pattern(regexp = "^[1-8]$", message = "Semester must be 1-8")
    @Column(nullable = false, length = 1)
    private String semester;
    
    @NotBlank(message = "Section is required")
    @Pattern(regexp = "^[A-Z]$", message = "Section must be a single uppercase letter")
    @Column(nullable = false, length = 1)
    private String section;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
