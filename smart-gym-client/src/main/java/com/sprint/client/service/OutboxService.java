package com.sprint.client.service;

import com.sprint.client.model.OutboxEvent;
import com.sprint.client.model.OutboxStatus;
import com.sprint.client.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    /*
     * Propagation.MANDATORY ensures this method is always called within an existing transaction.
     * Outbox event and business data must be saved atomically - either both commit or both rollback.
     * If no active transaction exists, Spring throws IllegalTransactionStateException immediately.
    */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(UUID eventId, String aggregateType, String aggregateId,
                          String eventType, String routingKey, Object payload) {
        try {
            String jsonPayload = jsonMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(eventId)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .routingKey(routingKey)
                    .payload(jsonPayload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Saved outbox event [{}] for aggregate [{}:{}]", eventType, aggregateType, aggregateId);

        } catch (JacksonException e) {
            log.error("Failed to serialize outbox event payload for aggregate [{}:{}]", aggregateType, aggregateId, e);
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }

    }
}
