package com.sprint.smartgymcore.messaging.inbox.repository;

import com.sprint.smartgymcore.messaging.inbox.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
