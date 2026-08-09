package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvigilatorRepository extends JpaRepository<Invigilator, Long> {
    List<Invigilator> findByUserOrderByInvigilatorId(AppUser user);
    Optional<Invigilator> findByIdAndUser(Long id, AppUser user);
    boolean existsByUserAndInvigilatorIdIgnoreCase(AppUser user, String invigilatorId);
    long countByUser(AppUser user);
}
