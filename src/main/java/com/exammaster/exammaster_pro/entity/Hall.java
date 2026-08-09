package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "halls", 
    uniqueConstraints = @UniqueConstraint(name = "uk_hall_user_name", columnNames = {"user_id", "hall_name"}),
    indexes = @Index(name = "idx_hall_user_building", columnList = "user_id, building_id")
)
public class Hall extends OwnedEntity {
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @NotBlank(message = "Hall name is required")
    @Pattern(regexp = "^[A-Za-z0-9._-]{1,50}$", message = "Hall name must be 1-50 characters using letters, numbers, dot, underscore, or hyphen; spaces are not allowed")
    @Column(name = "hall_name", nullable = false, length = 50)
    private String hallName;

    @Min(value = 1, message = "Bench count must be at least 1")
    @Max(value = 500, message = "Bench count cannot exceed 500")
    @Column(name = "bench_count", nullable = false)
    private int benchCount;

    @NotNull(message = "Students per bench is required")
    @Min(value = 1, message = "Students per bench must be 1 or 2")
    @Max(value = 2, message = "Students per bench must be 1 or 2")
    @Column(name = "students_per_bench", nullable = false)
    private int studentsPerBench;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private int capacity;
    
    @PrePersist
    @PreUpdate
    protected void calculateCapacity() {
        this.capacity = benchCount * studentsPerBench;
    }
}
