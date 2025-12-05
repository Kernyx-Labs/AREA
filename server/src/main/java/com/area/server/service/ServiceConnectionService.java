package com.area.server.service;

import com.area.server.model.ServiceConnection;
import com.area.server.repository.ServiceConnectionRepository;
import org.springframework.stereotype.Service;

@Service
public class ServiceConnectionService {

    private final ServiceConnectionRepository repository;

    public ServiceConnectionService(ServiceConnectionRepository repository) {
        this.repository = repository;
    }

    public ServiceConnection create(ServiceConnection connection) {
        return repository.save(connection);
    }

    public ServiceConnection findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Service connection not found: " + id));
    }

    public Iterable<ServiceConnection> list() {
        return repository.findAll();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

