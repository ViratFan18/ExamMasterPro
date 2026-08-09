package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "allocations", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_allocation_student_exam", columnNames = {"user_id", "exam_id", "student_id"}),
        @UniqueConstraint(name = "uk_allocation_seat_exam", columnNames = {"user_id", "exam_id", "hall_id", "seat_number"})
    },
    indexes = {
        @Index(name = "idx_allocation_user_exam", columnList = "user_id, exam_id"),
        @Index(name = "idx_allocation_student", columnList = "user_id, student_id")
    }
)
public class Allocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    @Column(name = "version")
    private Integer version;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;
    
    @NotBlank(message = "Seat number is required")
    @Pattern(regexp = "^B\\d{2}-S[1-2]$", message = "Seat number must be in format B##-S# (e.g., B01-S1)")
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;
    
    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt = Instant.now();
}
