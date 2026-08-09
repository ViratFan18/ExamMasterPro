package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.Student;
import com.exammaster.exammaster_pro.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepositoryOptimized extends JpaRepository<Student, Long> {
    
    /**
     * Find all students for a user with pagination to prevent memory overflow
     */
    @EntityGraph(attributePaths = {})
    Page<Student> findByUserOrderByHallTicketNumber(AppUser user, Pageable pageable);
    
    /**
     * Find all students (eager load everything)
     */
    @EntityGraph(attributePaths = {})
    List<Student> findByUser(AppUser user);
    
    /**
     * Find students by branch with pagination
     */
    @EntityGraph(attributePaths = {})
    Page<Student> findByUserAndBranchOrderBySection(AppUser user, String branch, Pageable pageable);
    
    /**
     * Count total students for capacity planning
     */
    long countByUser(AppUser user);
    
    /**
     * Get student count by branch for analytics
     */
    @Query("SELECT s.branch, COUNT(s) FROM Student s WHERE s.user = :user GROUP BY s.branch ORDER BY COUNT(s) DESC")
    List<Object[]> countByBranch(@Param("user") AppUser user);
    
    /**
     * Validate hall ticket uniqueness before import
     */
    @Query("SELECT COUNT(s) > 0 FROM Student s WHERE s.user = :user AND UPPER(s.hallTicketNumber) = UPPER(:ticket)")
    boolean existsByUserAndTicket(@Param("user") AppUser user, @Param("ticket") String ticket);
    
    /**
     * Find students with their full details (for allocation)
     */
    @Query("SELECT s FROM Student s WHERE s.user = :user ORDER BY s.section, s.branch, s.hallTicketNumber")
    List<Student> findAllForAllocation(@Param("user") AppUser user);
}
