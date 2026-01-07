package com.area.server.controller;

import com.area.server.dto.response.ApiResponse;
import com.area.server.model.Area;
import com.area.server.model.AreaExecutionLog;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.AreaExecutionLogRepository;
import com.area.server.repository.AreaRepository;
import com.area.server.repository.ServiceConnectionRepository;
import org.springframework.data.domain.PageRequest;
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        // Use optimized count queries instead of loading all data into memory

        // Get area counts
        long totalAreas = areaRepository.count();
        long activeAreas = areaRepository.findByActiveTrue().size(); // Already optimized query
        long inactiveAreas = totalAreas - activeAreas;

        // Get service connections count
        long connectedServices = serviceConnectionRepository.count();

        // Get execution statistics for last 24 hours using indexed queries
        Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);
        long executionsLast24h = executionLogRepository.countByExecutedAtAfter(last24Hours);
        long successfulExecutions = executionLogRepository.countByStatusAndExecutedAtAfter(
            AreaExecutionLog.ExecutionStatus.SUCCESS, last24Hours);
        long failedExecutions = executionLogRepository.countByStatusAndExecutedAtAfter(
            AreaExecutionLog.ExecutionStatus.FAILURE, last24Hours);

        // Calculate success rate
        double successRate = executionsLast24h > 0
            ? (double) successfulExecutions / executionsLast24h * 100
            : 0.0;

        // Get execution trend (last 7 days vs previous 7 days)
        Instant last7Days = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant previous7DaysStart = Instant.now().minus(14, ChronoUnit.DAYS);

        long currentWeekExecutions = executionLogRepository.countByExecutedAtAfter(last7Days);
        // For previous week, we need to count between previous7DaysStart and last7Days
        List<AreaExecutionLog> previousWeekLogs = executionLogRepository
            .findByExecutedAtBetween(previous7DaysStart, last7Days, PageRequest.of(0, Integer.MAX_VALUE))
            .getContent();
        long previousWeekExecutions = previousWeekLogs.size();

        // Calculate trend
        double executionTrend = previousWeekExecutions > 0
            ? ((double) (currentWeekExecutions - previousWeekExecutions) / previousWeekExecutions * 100)
            : 0.0;

        // Get most active areas (top 5) from last 24 hours
        List<AreaExecutionLog> recentLogs = executionLogRepository.findRecentLogs(
            last24Hours, PageRequest.of(0, 1000)); // Limit to 1000 most recent

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

        // Get recent activity (last 10 executions) using indexed query
        List<Map<String, Object>> recentActivity = executionLogRepository
            .findRecentLogs(Instant.now().minus(30, ChronoUnit.DAYS), PageRequest.of(0, 10))
            .stream()
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

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
