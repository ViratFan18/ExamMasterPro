package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.Allocation;
import com.exammaster.exammaster_pro.entity.AppUser;
import com.exammaster.exammaster_pro.entity.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AllocationRepositoryOptimized extends JpaRepository<Allocation, Long> {
    
    /**
     * Get allocations for an exam with eager loading to prevent N+1
     */
    @EntityGraph(attributePaths = {"student", "exam", "hall", "building"})
    Page<Allocation> findByUserAndExamOrderByHallHallNameAscSeatNumberAsc(
        AppUser user, Exam exam, Pageable pageable);
    
    /**
     * Get full allocation details without pagination (for download/export)
     */
    @EntityGraph(attributePaths = {"student", "exam", "hall", "building"})
    List<Allocation> findByUserAndExam(AppUser user, Exam exam);
    
    /**
     * Check if allocations exist for an exam
     */
    long countByUserAndExam(AppUser user, Exam exam);
    
    /**
     * Find allocation for a specific student-exam pair
     */
    @EntityGraph(attributePaths = {"hall", "building"})
    @Query("SELECT a FROM Allocation a WHERE a.user = :user AND a.exam = :exam AND a.student.id = :studentId")
    java.util.Optional<Allocation> findByUserExamAndStudent(
        @Param("user") AppUser user, 
        @Param("exam") Exam exam, 
        @Param("studentId") Long studentId);
    
    /**
     * Get hallway-wise allocation summary
     */
    @Query("""
        SELECT a.hall.hallName, COUNT(a) as count
        FROM Allocation a 
        WHERE a.user = :user AND a.exam = :exam
        GROUP BY a.hall.hallName
        ORDER BY a.hall.hallName
    """)
    List<Object[]> getAllocationSummaryByHall(@Param("user") AppUser user, @Param("exam") Exam exam);
    
    /**
     * Get branch-wise allocation distribution
     */
    @Query("""
        SELECT a.student.branch, COUNT(a) as count
        FROM Allocation a 
        WHERE a.user = :user AND a.exam = :exam
        GROUP BY a.student.branch
        ORDER BY COUNT(a) DESC
    """)
    List<Object[]> getAllocationByBranch(@Param("user") AppUser user, @Param("exam") Exam exam);
}
