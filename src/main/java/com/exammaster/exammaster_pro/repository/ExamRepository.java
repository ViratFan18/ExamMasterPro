package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByUserOrderByCreatedAtDesc(AppUser user);
    Optional<Exam> findByIdAndUser(Long id, AppUser user);
    boolean existsByUserAndExamNameIgnoreCaseAndSemesterIgnoreCaseAndExamType(AppUser user, String examName, String semester, ExamType examType);
    long countByUser(AppUser user);
}
