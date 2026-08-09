package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserOrderByCreatedAtDesc(AppUser user);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    Optional<Complaint> findByIdAndUser(Long id, AppUser user);
}
