package com.area.server.service;

import com.area.server.exception.ServiceConnectionInUseException;
import com.area.server.exception.ServiceConnectionNotFoundException;
import com.area.server.model.Area;
import com.area.server.model.ServiceConnection;
import com.area.server.repository.AreaRepository;
import com.area.server.repository.ServiceConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceConnectionService.class);

    private final ServiceConnectionRepository repository;
    private final AreaRepository areaRepository;

    public ServiceConnectionService(ServiceConnectionRepository repository, AreaRepository areaRepository) {
        this.repository = repository;
        this.areaRepository = areaRepository;
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

    /**
     * Delete a service connection after validating it's not in use by any areas.
     * Throws ServiceConnectionInUseException if the connection is referenced by active areas.
     *
     * @param id The ID of the connection to delete
     * @throws ServiceConnectionNotFoundException if connection doesn't exist
     * @throws ServiceConnectionInUseException if connection is being used by areas
     */
    public void delete(Long id) {
        // Verify connection exists
        ServiceConnection connection = findById(id);

        // Check if connection is in use
        List<Area> areasUsingConnection = getAreasUsingConnection(id);

        if (!areasUsingConnection.isEmpty()) {
            List<Long> areaIds = areasUsingConnection.stream()
                .map(Area::getId)
                .collect(Collectors.toList());

            logger.warn("Attempted to delete service connection {} but {} area(s) are still using it: {}",
                id, areaIds.size(), areaIds);

            throw new ServiceConnectionInUseException(id, areaIds);
        }

        logger.info("Deleting service connection {} (type: {})", id, connection.getType());
        repository.deleteById(id);
    }

    /**
     * Check if a service connection can be safely deleted.
     * Returns true if no areas are using this connection.
     *
     * @param connectionId The ID of the connection to check
     * @return true if connection can be deleted, false otherwise
     */
    public boolean canDelete(Long connectionId) {
        List<Area> areasUsingConnection = getAreasUsingConnection(connectionId);
        return areasUsingConnection.isEmpty();
    }

    /**
     * Get all areas that are using the specified service connection.
     * This includes areas where the connection is used as either action or reaction.
     *
     * @param connectionId The ID of the connection to check
     * @return List of areas using this connection
     */
    public List<Area> getAreasUsingConnection(Long connectionId) {
        return areaRepository.findByActionConnectionIdOrReactionConnectionId(connectionId);
    }
}

