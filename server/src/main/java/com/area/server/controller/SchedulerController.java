package com.area.server.controller;

import com.area.server.dto.response.ApiResponse;
import com.area.server.scheduler.WorkflowPollingScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final WorkflowPollingScheduler scheduler;

    public SchedulerController(WorkflowPollingScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        long last = scheduler.getLastExecutionTime();
        long interval = scheduler.getPollingInterval();

        status.put("lastExecutionTime", last);
        status.put("interval", interval);
        status.put("nextExecutionTime", last + interval);

        return ResponseEntity.ok(ApiResponse.success("Scheduler status retrieved", status));
    }
}
