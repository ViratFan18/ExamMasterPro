package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvigilatorAllocationRepository extends JpaRepository<InvigilatorAllocation, Long> {
    List<InvigilatorAllocation> findByUserAndExamOrderByHallHallName(AppUser user, Exam exam);
    void deleteByUserAndExam(AppUser user, Exam exam);
}
