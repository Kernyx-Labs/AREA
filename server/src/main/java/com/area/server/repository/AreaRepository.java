package com.area.server.repository;

import com.area.server.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findByActiveTrue();

    // Find active areas that are Timer based (type starts with 'timer.' or
    // actionConnection type is TIMER)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Area a LEFT JOIN a.actionConnection ac WHERE a.active = true AND (a.actionType LIKE 'timer.%' OR (ac IS NOT NULL AND ac.type = 'TIMER'))")
    List<Area> findActiveTimerAreas();

    // Find active areas that are NOT Timer based
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Area a LEFT JOIN a.actionConnection ac WHERE a.active = true AND (a.actionType NOT LIKE 'timer.%' OR a.actionType IS NULL) AND (ac IS NULL OR ac.type != 'TIMER')")
    List<Area> findActiveNonTimerAreas();
}
