package com.area.server.repository;

import com.area.server.model.AreaExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AreaExecutionLogRepository extends JpaRepository<AreaExecutionLog, Long> {

    Page<AreaExecutionLog> findByAreaId(Long areaId, Pageable pageable);

    List<AreaExecutionLog> findByAreaIdAndExecutedAtAfter(Long areaId, Instant after);

    void deleteByAreaId(Long areaId);
}
