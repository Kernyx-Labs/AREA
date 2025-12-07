package com.area.server.repository;

import com.area.server.model.AreaTriggerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AreaTriggerStateRepository extends JpaRepository<AreaTriggerState, Long> {

    Optional<AreaTriggerState> findByAreaId(Long areaId);

    void deleteByAreaId(Long areaId);
}
