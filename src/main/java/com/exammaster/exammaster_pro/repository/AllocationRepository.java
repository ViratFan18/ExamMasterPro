package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByUserAndExamOrderByHallHallNameAscSeatNumberAsc(AppUser user, Exam exam);
    List<Allocation> findByUserAndExamAndBuildingOrderByHallHallNameAscSeatNumberAsc(AppUser user, Exam exam, Building building);
    List<Allocation> findByUserAndExamAndHallOrderBySeatNumberAsc(AppUser user, Exam exam, Hall hall);
    List<Allocation> findByUserAndHallOrderBySeatNumberAsc(AppUser user, Hall hall);
    long countByUser(AppUser user);
    long countByUserAndExam(AppUser user, Exam exam);
    long countByUserAndHall(AppUser user, Hall hall);
    boolean existsByUserAndExam(AppUser user, Exam exam);
    void deleteByUserAndExam(AppUser user, Exam exam);
    Optional<Allocation> findByUserAndExamAndStudent(AppUser user, Exam exam, Student student);
}
