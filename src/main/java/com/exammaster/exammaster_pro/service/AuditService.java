package com.exammaster.exammaster_pro.service;

import com.exammaster.exammaster_pro.dto.Responses.AuditResponse;
import com.exammaster.exammaster_pro.entity.AppUser;
import com.exammaster.exammaster_pro.entity.AuditRecord;
import com.exammaster.exammaster_pro.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private static final int MAX_AUDIT_RECORDS_PER_USER = 10;
    private final AuditRecordRepository audits;
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Transactional
    public void log(AppUser user, String action, String module, String description, String performedBy) {
        AuditRecord record = new AuditRecord();
        record.setUser(user);
        record.setAction(action);
        record.setModule(module);
        record.setDescription(description);
        record.setPerformedBy(performedBy);
        audits.save(record);
        trimUserHistory(user);
        log.info("Audit: user='{}' action='{}' module='{}' by='{}'", user == null ? "anonymous" : user.getUsername(), action, module, performedBy);
    }

    public List<AuditResponse> list(AppUser user) {
        return audits.findTop10ByUserOrderByPerformedAtDesc(user).stream().map(this::toResponse).toList();
    }

    public List<AuditResponse> recent(AppUser user) {
        return audits.findTop10ByUserOrderByPerformedAtDesc(user).stream().map(this::toResponse).toList();
    }

    private void trimUserHistory(AppUser user) {
        List<AuditRecord> records = audits.findByUserOrderByPerformedAtDesc(user);
        if (records.size() <= MAX_AUDIT_RECORDS_PER_USER) {
            return;
        }
        audits.deleteAll(records.subList(MAX_AUDIT_RECORDS_PER_USER, records.size()));
        log.info("Trimmed {} old audit records for user='{}'.", Math.max(0, records.size() - MAX_AUDIT_RECORDS_PER_USER), user == null ? "anonymous" : user.getUsername());
    }

    private AuditResponse toResponse(AuditRecord a) {
        return new AuditResponse(a.getId(), a.getAction(), a.getModule(), a.getDescription(), a.getPerformedBy(), a.getPerformedAt());
    }
}
