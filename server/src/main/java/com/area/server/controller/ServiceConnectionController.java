package com.area.server.controller;

import com.area.server.controller.dto.CreateServiceConnectionRequest;
import com.area.server.model.ServiceConnection;
import com.area.server.service.ServiceConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service-connections")
public class ServiceConnectionController {

    private final ServiceConnectionService serviceConnectionService;

    public ServiceConnectionController(ServiceConnectionService serviceConnectionService) {
        this.serviceConnectionService = serviceConnectionService;
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
}
