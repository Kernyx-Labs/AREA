package com.area.server.repository;

import com.area.server.model.AreaExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AreaExecutionLogRepository extends JpaRepository<AreaExecutionLog, Long> {

    // Existing methods
    Page<AreaExecutionLog> findByAreaId(Long areaId, Pageable pageable);

    List<AreaExecutionLog> findByAreaIdAndExecutedAtAfter(Long areaId, Instant after);

    void deleteByAreaId(Long areaId);

    // New methods for global logs filtering
    Page<AreaExecutionLog> findByStatus(AreaExecutionLog.ExecutionStatus status, Pageable pageable);

    Page<AreaExecutionLog> findByExecutedAtBetween(Instant start, Instant end, Pageable pageable);

    Page<AreaExecutionLog> findByAreaIdAndStatus(Long areaId, AreaExecutionLog.ExecutionStatus status, Pageable pageable);

    Page<AreaExecutionLog> findByAreaIdAndExecutedAtBetween(Long areaId, Instant start, Instant end, Pageable pageable);

    Page<AreaExecutionLog> findByStatusAndExecutedAtBetween(
        AreaExecutionLog.ExecutionStatus status, Instant start, Instant end, Pageable pageable);

    Page<AreaExecutionLog> findByAreaIdAndStatusAndExecutedAtBetween(
        Long areaId, AreaExecutionLog.ExecutionStatus status, Instant start, Instant end, Pageable pageable);

    // Optimized count queries for dashboard statistics
    @Query("SELECT COUNT(l) FROM AreaExecutionLog l WHERE l.executedAt > :after")
    long countByExecutedAtAfter(@Param("after") Instant after);

    @Query("SELECT COUNT(l) FROM AreaExecutionLog l WHERE l.status = :status AND l.executedAt > :after")
    long countByStatusAndExecutedAtAfter(@Param("status") AreaExecutionLog.ExecutionStatus status, @Param("after") Instant after);

    @Query("SELECT l FROM AreaExecutionLog l WHERE l.executedAt > :after ORDER BY l.executedAt DESC")
    List<AreaExecutionLog> findRecentLogs(@Param("after") Instant after, Pageable pageable);
}
