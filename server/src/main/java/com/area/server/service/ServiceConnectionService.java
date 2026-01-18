package com.area.server.service;

import com.area.server.exception.ServiceConnectionNotFoundException;
import com.area.server.model.ServiceConnection;
import com.area.server.model.User;
import com.area.server.repository.ServiceConnectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
                .orElseThrow(() -> new ServiceConnectionNotFoundException(id));
    }

    public Iterable<ServiceConnection> list() {
        return repository.findAll();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Find all service connections for a user of a specific type
     *
     * @param user The user entity
     * @param type The service type
     * @return List of service connections
     */
    public List<ServiceConnection> findByUserAndType(User user, ServiceConnection.ServiceType type) {
        return repository.findByUserAndType(user, type);
    }

    /**
     * Find first service connection for a user of a specific type
     * Useful when only one connection per service type is expected
     *
     * @param user The user entity
     * @param type The service type
     * @return Optional containing the first service connection if found
     */
    public Optional<ServiceConnection> findFirstByUserAndType(User user, ServiceConnection.ServiceType type) {
        return repository.findFirstByUserAndType(user, type);
    }
}

