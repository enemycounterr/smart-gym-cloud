package com.sprint.smartgymcore.messaging.outbox.service;

import com.sprint.smartgymcore.messaging.outbox.model.OutboxEvent;
import com.sprint.smartgymcore.messaging.outbox.model.OutboxStatus;
import com.sprint.smartgymcore.messaging.outbox.repository.OutboxRepository;
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

    @Transactional
    public int deleteSentEventsBatch(Instant cutoff, int batchSize) {
        return outboxRepository.deleteSentEventsOlderThanBatch(cutoff, batchSize);
    }
}

