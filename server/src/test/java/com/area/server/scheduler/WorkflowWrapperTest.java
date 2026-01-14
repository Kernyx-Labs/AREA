package com.area.server.scheduler;

import com.area.server.dto.WorkflowData;
import com.area.server.model.GitHubActionConfig;
import com.area.server.model.Workflow;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorkflowWrapperTest {

    @Test
    public void testGetGithubActionConfig_WithCombinedRepositoryField() {
        // Arrange
        Workflow workflow = new Workflow();
        WorkflowData workflowData = new WorkflowData();
        WorkflowData.TriggerConfig trigger = new WorkflowData.TriggerConfig();
        trigger.setService("github");
        trigger.setType("issue_created");

        Map<String, Object> config = new HashMap<>();
        config.put("repository", "test-owner/test-repo"); // The field that was being ignored
        trigger.setConfig(config);

        workflowData.setTrigger(trigger);

        WorkflowWrapper wrapper = new WorkflowWrapper(workflow, workflowData, null, null);

        // Act
        GitHubActionConfig result = wrapper.getGithubActionConfig();

        // Assert
        assertNotNull(result);
        assertEquals("issue_created", result.getActionType());

        // These should be populated by the setRepository() logic inside
        // GitHubActionConfig
        // via the fix in WorkflowWrapper calling setRepository
        assertEquals("test-owner", result.getRepositoryOwner());
        assertEquals("test-repo", result.getRepositoryName());
    }

    @Test
    public void testGetGithubActionConfig_WithLegacyFields() {
        // Arrange
        Workflow workflow = new Workflow();
        WorkflowData workflowData = new WorkflowData();
        WorkflowData.TriggerConfig trigger = new WorkflowData.TriggerConfig();
        trigger.setService("github");
        trigger.setType("pr_created");

        Map<String, Object> config = new HashMap<>();
        config.put("repositoryOwner", "legacy-owner");
        config.put("repositoryName", "legacy-repo");
        trigger.setConfig(config);

        workflowData.setTrigger(trigger);

        WorkflowWrapper wrapper = new WorkflowWrapper(workflow, workflowData, null, null);

        // Act
        GitHubActionConfig result = wrapper.getGithubActionConfig();

        // Assert
        assertNotNull(result);
        assertEquals("pr_created", result.getActionType());
        assertEquals("legacy-owner", result.getRepositoryOwner());
        assertEquals("legacy-repo", result.getRepositoryName());
    }
}
