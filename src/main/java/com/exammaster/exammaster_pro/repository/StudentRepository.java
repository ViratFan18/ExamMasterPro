package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByUserOrderByHallTicketNumber(AppUser user);
    Optional<Student> findByIdAndUser(Long id, AppUser user);
    Optional<Student> findByUserAndHallTicketNumberIgnoreCase(AppUser user, String hallTicketNumber);
    boolean existsByUserAndHallTicketNumberIgnoreCase(AppUser user, String hallTicketNumber);
    long countByUser(AppUser user);
}
