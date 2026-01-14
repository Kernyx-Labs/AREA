package com.area.server.repository;

import com.area.server.model.WorkflowTriggerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowTriggerStateRepository extends JpaRepository<WorkflowTriggerState, Long> {
    Optional<WorkflowTriggerState> findByWorkflowId(Long workflowId);
}
