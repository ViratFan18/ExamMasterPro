package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    List<AuditRecord> findTop10ByUserOrderByPerformedAtDesc(AppUser user);
    List<AuditRecord> findByUserOrderByPerformedAtDesc(AppUser user);
}
