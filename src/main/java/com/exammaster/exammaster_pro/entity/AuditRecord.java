package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_records")
public class AuditRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String module;
    @Column(nullable = false, length = 1500)
    private String description;
    @Column(name = "performed_by", nullable = false)
    private String performedBy;
    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt = Instant.now();
}
