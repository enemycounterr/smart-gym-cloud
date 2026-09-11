package com.sprint.smartgymcore.repository;

import com.sprint.smartgymcore.model.AccessZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessZoneRepository extends JpaRepository<AccessZone, Long> {
    Optional<AccessZone> findByZoneName(String zoneName);

    @Query("SELECT z FROM AccessZone z JOIN z.clientIds c WHERE c = :clientId")
    List<AccessZone> findAllByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COUNT(z) > 0 FROM AccessZone z JOIN z.clientIds c WHERE z.id = :zoneId AND c = :clientId")
    boolean hasClientAccess(@Param("zoneId") Long zoneId, @Param("clientId") Long clientId);
}
