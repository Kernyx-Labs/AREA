package com.area.server.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkflowDataTest {

    @Test
    public void testGetFullType_NoPrefix() {
        WorkflowData.TriggerConfig trigger = new WorkflowData.TriggerConfig();
        trigger.setService("GitHub");
        trigger.setType("issue_created");
        assertEquals("github.issue_created", trigger.getFullType());
    }

    @Test
    public void testGetFullType_WithPrefix() {
        WorkflowData.TriggerConfig trigger = new WorkflowData.TriggerConfig();
        trigger.setService("GitHub");
        trigger.setType("github.issue_created");
        assertEquals("github.issue_created", trigger.getFullType());
    }

    @Test
    public void testGetFullType_WithPrefixLowercase() {
        WorkflowData.TriggerConfig trigger = new WorkflowData.TriggerConfig();
        trigger.setService("github");
        trigger.setType("github.issue_created");
        assertEquals("github.issue_created", trigger.getFullType());
    }
}
