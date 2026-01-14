package com.area.server.repository;

import com.area.server.model.ServiceConnection;
import com.area.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceConnectionRepository extends JpaRepository<ServiceConnection, Long> {

    /**
     * Find all service connections for a user of a specific type
     *
     * @param user The user entity
     * @param type The service type
     * @return List of service connections
     */
    List<ServiceConnection> findByUserAndType(User user, ServiceConnection.ServiceType type);

    /**
     * Find first service connection for a user of a specific type
     * Useful when only one connection per service type is expected
     *
     * @param user The user entity
     * @param type The service type
     * @return Optional containing the first service connection if found
     */
    Optional<ServiceConnection> findFirstByUserAndType(User user, ServiceConnection.ServiceType type);
}

