package com.area.server.controller;

import com.area.server.controller.dto.CreateServiceConnectionRequest;
import com.area.server.dto.response.ApiResponse;
import com.area.server.model.ServiceConnection;
import com.area.server.service.ServiceConnectionService;
import com.area.server.service.TokenRefreshService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/service-connections")
public class ServiceConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConnectionController.class);

    private final ServiceConnectionService serviceConnectionService;
    private final TokenRefreshService tokenRefreshService;

    public ServiceConnectionController(ServiceConnectionService serviceConnectionService,
                                      TokenRefreshService tokenRefreshService) {
        this.serviceConnectionService = serviceConnectionService;
        this.tokenRefreshService = tokenRefreshService;
    }

    @PostMapping
    public ResponseEntity<ServiceConnection> create(@Valid @RequestBody CreateServiceConnectionRequest request) {
        ServiceConnection connection = new ServiceConnection();
        connection.setType(request.getType());
        connection.setAccessToken(request.getAccessToken());
        connection.setRefreshToken(request.getRefreshToken());
        connection.setExpiresInSeconds(request.getExpiresInSeconds());
        connection.setMetadata(request.getMetadata());
        return ResponseEntity.ok(serviceConnectionService.create(connection));
    }

    @GetMapping
    public Iterable<ServiceConnection> list() {
        return serviceConnectionService.list();
    }

    /**
     * Check if a service connection can be safely deleted.
     * Returns information about whether the connection is in use by any areas.
     */
    @GetMapping("/{id}/can-delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canDelete(@PathVariable Long id) {
        // Verify connection exists (throws ServiceConnectionNotFoundException if not)
        serviceConnectionService.findById(id);

        boolean canDelete = serviceConnectionService.canDelete(id);
        var areasUsingConnection = serviceConnectionService.getAreasUsingConnection(id);

        Map<String, Object> data = Map.of(
            "canDelete", canDelete,
            "connectionId", id,
            "areasInUse", areasUsingConnection.stream()
                .map(area -> Map.of(
                    "id", area.getId(),
                    "active", area.isActive()
                ))
                .toList(),
            "areaCount", areasUsingConnection.size()
        );

        String message = canDelete
            ? "Connection can be safely deleted"
            : String.format("Connection is being used by %d area(s)", areasUsingConnection.size());

        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceConnectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually refresh OAuth token for a service connection
     * Uses GlobalExceptionHandler for error handling - no try-catch needed
     * Throws ResourceNotFoundException if connection not found
     * Throws ValidationException if service doesn't support token refresh
     */
    @PostMapping("/{id}/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@PathVariable Long id) {
        ServiceConnection connection = serviceConnectionService.findById(id);

        if (connection.getType() != ServiceConnection.ServiceType.GMAIL) {
            throw new IllegalArgumentException("Only Gmail connections support token refresh");
        }

        ServiceConnection refreshed = tokenRefreshService.refreshTokenIfNeeded(connection)
            .blockOptional()
            .orElseThrow(() -> new RuntimeException("Failed to refresh token"));

        logger.info("Token refreshed successfully for connection {}", id);

        Map<String, Object> data = Map.of("connection", refreshed);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", data));
    }
}
