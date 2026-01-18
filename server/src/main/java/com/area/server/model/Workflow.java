package com.area.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "workflows")
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // ServiceConnection for the trigger (action)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_connection_id")
    private ServiceConnection triggerConnection;

    // ServiceConnection for reactions (may be null if reactions don't need auth)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reaction_connection_id")
    private ServiceConnection reactionConnection;

    // Workflow configuration stored as JSON
    @Column(columnDefinition = "TEXT")
    private String workflowData; // JSON: { trigger, actions, reactions, connections }

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<WorkflowExecutionLog> executionLogs = new ArrayList<>();

    @OneToOne(mappedBy = "workflow", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private WorkflowTriggerState triggerState;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getWorkflowData() {
        return workflowData;
    }

    public void setWorkflowData(String workflowData) {
        this.workflowData = workflowData;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceConnection getTriggerConnection() {
        return triggerConnection;
    }

    public void setTriggerConnection(ServiceConnection triggerConnection) {
        this.triggerConnection = triggerConnection;
    }

    public ServiceConnection getReactionConnection() {
        return reactionConnection;
    }

    public void setReactionConnection(ServiceConnection reactionConnection) {
        this.reactionConnection = reactionConnection;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
