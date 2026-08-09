package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "exams", uniqueConstraints = @UniqueConstraint(name = "uk_exam_user_scope", columnNames = {"user_id", "exam_name", "semester", "exam_type"}))
public class Exam extends OwnedEntity {
    @Column(name = "exam_name", nullable = false)
    private String examName;
    @Column(name = "academic_year", nullable = false)
    private String academicYear;
    @Column(nullable = false)
    private String semester;
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private ExamType examType;
}
