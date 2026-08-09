package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "complaints")
public class Complaint extends OwnedEntity {
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 3000)
    private String description;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.OPEN;
}
