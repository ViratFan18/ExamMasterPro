package com.exammaster.exammaster_pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "buildings", uniqueConstraints = @UniqueConstraint(name = "uk_building_user_name", columnNames = {"user_id", "building_name"}))
public class Building extends OwnedEntity {
    @Column(name = "building_name", nullable = false)
    private String buildingName;

    @Column(name = "max_hall_count", nullable = false)
    private int maxHallCount;
}
