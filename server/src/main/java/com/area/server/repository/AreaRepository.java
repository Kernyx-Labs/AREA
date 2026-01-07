package com.area.server.repository;

import com.area.server.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findByActiveTrue();

    /**
     * Find all areas that use the specified service connection as either their action or reaction connection.
     * This is used to check if a service connection can be safely deleted.
     *
     * @param connectionId The ID of the service connection to check
     * @return List of areas that reference this connection
     */
    @Query("SELECT a FROM Area a WHERE a.actionConnection.id = :connectionId OR a.reactionConnection.id = :connectionId")
    List<Area> findByActionConnectionIdOrReactionConnectionId(@Param("connectionId") Long connectionId);
}

