package com.sprint.smartgymcore.repository;

import com.sprint.smartgymcore.dto.access.AccessLogResponse;
import com.sprint.smartgymcore.model.AccessDirection;
import com.sprint.smartgymcore.model.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    List<AccessLog> findAllByClientId(Long clientId);

    boolean existsByAccessZoneId(Long zoneId);

    @EntityGraph(attributePaths = {"accessZone"})
    @Query("SELECT l FROM AccessLog l")
    List<AccessLog> findAllWithClientAndZone();

    long countByClientIdAndDirection(Long clientId, AccessDirection direction);

    @Query(
            "SELECT l FROM AccessLog l WHERE l.timeStamp = " +
                    "(SELECT MAX(sub.timeStamp) FROM AccessLog sub WHERE sub.clientId = l.clientId) " +
                    "AND l.direction = :direction"
    )
    List<AccessLog> findCurrentClientsInside(@Param("direction") AccessDirection direction);

    Optional<AccessLog> findFirstByClientIdOrderByTimeStampDesc(Long clientId);

    @Query("""
        SELECT 
            l.id AS logId, 
            l.clientId AS clientId, 
            COALESCE(c.clientName, 'Unknown Client') AS clientName, 
            z.zoneName AS zoneName, 
            l.direction AS direction, 
            l.timeStamp AS timestamp
        FROM AccessLog l
        JOIN l.accessZone z
        LEFT JOIN AccessCard c ON l.clientId = c.clientId
    """)
    Page<AccessLogResponse> findAllAccessLogsWithClientName(Pageable pageable);
}
