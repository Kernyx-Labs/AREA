package com.area.server.controller;

import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.repository.ServiceConnectionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AreaRepository areaRepository;
    private final ServiceConnectionRepository serviceConnectionRepository;
    private final AreaExecutionLogRepository executionLogRepository;

    public DashboardController(AreaRepository areaRepository,
                               ServiceConnectionRepository serviceConnectionRepository,
                               AreaExecutionLogRepository executionLogRepository) {
        this.areaRepository = areaRepository;
        this.serviceConnectionRepository = serviceConnectionRepository;
        this.executionLogRepository = executionLogRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        // Get all areas
        List<Area> allAreas = areaRepository.findAll();
        long totalAreas = allAreas.size();
        long activeAreas = allAreas.stream().filter(Area::isActive).count();
        long inactiveAreas = totalAreas - activeAreas;

        // Get service connections
        long connectedServices = serviceConnectionRepository.count();

        // Get execution logs from last 24 hours
        Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);
        List<AreaExecutionLog> recentLogs = executionLogRepository.findAll().stream()
            .filter(log -> log.getExecutedAt() != null && log.getExecutedAt().isAfter(last24Hours))
            .collect(Collectors.toList());

        long executionsLast24h = recentLogs.size();
        long successfulExecutions = recentLogs.stream()
            .filter(log -> log.getStatus() == AreaExecutionLog.ExecutionStatus.SUCCESS)
            .count();
        long failedExecutions = recentLogs.stream()
            .filter(log -> log.getStatus() == AreaExecutionLog.ExecutionStatus.FAILURE)
            .count();

        // Calculate success rate
        double successRate = executionsLast24h > 0
            ? (double) successfulExecutions / executionsLast24h * 100
            : 0.0;

        // Get last 7 days execution trend
        Instant last7Days = Instant.now().minus(7, ChronoUnit.DAYS);
        List<AreaExecutionLog> last7DaysLogs = executionLogRepository.findAll().stream()
            .filter(log -> log.getExecutedAt() != null && log.getExecutedAt().isAfter(last7Days))
            .collect(Collectors.toList());

        // Get previous 7 days for comparison
        Instant previous7DaysStart = Instant.now().minus(14, ChronoUnit.DAYS);
        Instant previous7DaysEnd = last7Days;
        long previousWeekExecutions = executionLogRepository.findAll().stream()
            .filter(log -> log.getExecutedAt() != null
                && log.getExecutedAt().isAfter(previous7DaysStart)
                && log.getExecutedAt().isBefore(previous7DaysEnd))
            .count();

        // Calculate trend
        long currentWeekExecutions = last7DaysLogs.size();
        double executionTrend = previousWeekExecutions > 0
            ? ((double) (currentWeekExecutions - previousWeekExecutions) / previousWeekExecutions * 100)
            : 0.0;

        // Get most active areas (top 5)
        Map<Long, Long> areaExecutionCount = recentLogs.stream()
            .filter(log -> log.getArea() != null)
            .collect(Collectors.groupingBy(log -> log.getArea().getId(), Collectors.counting()));

        List<Map<String, Object>> topAreas = areaExecutionCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Optional<Area> area = areaRepository.findById(entry.getKey());
                Map<String, Object> map = new HashMap<>();
                map.put("areaId", entry.getKey());
                map.put("executionCount", entry.getValue());
                map.put("active", area.map(Area::isActive).orElse(false));
                return map;
            })
            .collect(Collectors.toList());

        // Get recent activity (last 10 executions)
        List<Map<String, Object>> recentActivity = executionLogRepository.findAll().stream()
            .sorted(Comparator.comparing(AreaExecutionLog::getExecutedAt).reversed())
            .limit(10)
            .map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", log.getId());
                map.put("areaId", log.getArea() != null ? log.getArea().getId() : null);
                map.put("status", log.getStatus().toString());
                map.put("success", log.getStatus() == AreaExecutionLog.ExecutionStatus.SUCCESS);
                map.put("message", log.getErrorMessage() != null ? log.getErrorMessage() :
                                   (log.getMessageSent() != null ? "Message sent successfully" : "Executed"));
                map.put("executedAt", log.getExecutedAt().toString());
                return map;
            })
            .collect(Collectors.toList());

        // Build response
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAreas", totalAreas);
        stats.put("activeAreas", activeAreas);
        stats.put("inactiveAreas", inactiveAreas);
        stats.put("connectedServices", connectedServices);
        stats.put("executionsLast24h", executionsLast24h);
        stats.put("successfulExecutions", successfulExecutions);
        stats.put("failedExecutions", failedExecutions);
        stats.put("successRate", Math.round(successRate * 10) / 10.0);
        stats.put("executionTrend", Math.round(executionTrend * 10) / 10.0);
        stats.put("topAreas", topAreas);
        stats.put("recentActivity", recentActivity);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "stats", stats
        ));
    }
}
