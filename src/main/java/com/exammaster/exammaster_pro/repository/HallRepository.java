package com.exammaster.exammaster_pro.repository;

import com.exammaster.exammaster_pro.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HallRepository extends JpaRepository<Hall, Long> {
    List<Hall> findByUserOrderByHallName(AppUser user);
    List<Hall> findByUserAndBuildingOrderByHallName(AppUser user, Building building);
    Optional<Hall> findByIdAndUser(Long id, AppUser user);
    boolean existsByUserAndHallNameIgnoreCase(AppUser user, String hallName);
    boolean existsByUserAndBuildingAndHallNameIgnoreCase(AppUser user, Building building, String hallName);
    long countByUser(AppUser user);
    long countByUserAndBuilding(AppUser user, Building building);
}
