package com.sprint.smartgymcore.messaging.outbox.repository;

import com.sprint.smartgymcore.messaging.outbox.model.OutboxEvent;
import com.sprint.smartgymcore.messaging.outbox.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Modifying
    @Query(value = """
            DELETE FROM outbox_events 
            WHERE id IN (
                SELECT id FROM outbox_events 
                WHERE status = 'SENT' AND sent_at < :cutoff 
                ORDER BY sent_at ASC 
                LIMIT :batchSize
            )
            """, nativeQuery = true)
    int deleteSentEventsOlderThanBatch(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
}
