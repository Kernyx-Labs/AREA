package com.area.server.controller;

import com.area.server.controller.dto.CreateServiceConnectionRequest;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceConnectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@PathVariable Long id) {
        try {
            ServiceConnection connection = serviceConnectionService.findById(id);

            if (connection.getType() != ServiceConnection.ServiceType.GMAIL) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Only Gmail connections support token refresh"
                ));
            }

            ServiceConnection refreshed = tokenRefreshService.refreshTokenIfNeeded(connection)
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("Failed to refresh token"));

            logger.info("Token refreshed successfully for connection {}", id);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Token refreshed successfully",
                "connection", refreshed
            ));

        } catch (IllegalStateException e) {
            logger.error("Failed to refresh token for connection {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Token refresh failed",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error refreshing token for connection {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to refresh token",
                "message", e.getMessage()
            ));
        }
    }
}
