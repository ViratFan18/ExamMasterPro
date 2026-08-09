package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invigilators", uniqueConstraints = @UniqueConstraint(name = "uk_invigilator_user_code", columnNames = {"user_id", "invigilator_id"}))
public class Invigilator extends OwnedEntity {
    @Column(name = "invigilator_id", nullable = false)
    private String invigilatorId;
    @Column(name = "invigilator_name", nullable = false)
    private String invigilatorName;
}
