package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "invigilator_allocations", uniqueConstraints = @UniqueConstraint(name = "uk_invig_hall_exam", columnNames = {"user_id", "exam_id", "hall_id"}))
public class InvigilatorAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invigilator_id_ref", nullable = false)
    private Invigilator invigilator;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();
}
