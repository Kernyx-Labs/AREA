package com.area.server.repository;

import com.area.server.model.WorkflowExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, Long> {
    Page<WorkflowExecutionLog> findByWorkflowIdOrderByExecutedAtDesc(Long workflowId, Pageable pageable);
}
