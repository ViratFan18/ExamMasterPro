package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    List<Building> findByUserOrderByBuildingName(AppUser user);
    Optional<Building> findByIdAndUser(Long id, AppUser user);
    Optional<Building> findByUserAndBuildingNameIgnoreCase(AppUser user, String buildingName);
    boolean existsByUserAndBuildingNameIgnoreCase(AppUser user, String buildingName);
    long countByUser(AppUser user);
}
