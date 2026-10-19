package com.sprint.smartgymcore.repository;

import com.sprint.smartgymcore.model.AccessCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AccessCardRepository extends JpaRepository<AccessCard, Long> {
    Optional<AccessCard> findByRfidToken(String rfidToken);

    Optional<AccessCard> findByClientId(Long clientId);

    List<AccessCard> findAllByClientName(String clientName);

    List<AccessCard> findAllByClientIdIn(@Param("clientIds") Set<Long> clientIds);
}
