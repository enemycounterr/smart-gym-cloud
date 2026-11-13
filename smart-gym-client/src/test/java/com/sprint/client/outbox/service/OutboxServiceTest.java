package com.sprint.client.outbox.service;

import com.sprint.client.messaging.event.ClientCreatedEvent;
import com.sprint.client.outbox.model.OutboxEvent;
import com.sprint.client.outbox.model.OutboxStatus;
import com.sprint.client.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        outboxService = new OutboxService(outboxRepository, jsonMapper);
    }

    @Test
    @DisplayName("saveEvent: should serialize payload to JSON and save OutboxEvent with PENDING status")
    void saveEvent_shouldSerializePayloadAndSavePendingEvent() {

        UUID eventId = UUID.randomUUID();
        ClientCreatedEvent payload = new ClientCreatedEvent(
                eventId, 42L, "Kaito", "kait@gym.com", Instant.parse("2026-11-12T00:00:00Z")
        );

        outboxService.saveEvent(
                eventId,
                "CLIENT",
                "42",
                "ClientCreated",
                "client.created",
                payload
        );

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals(eventId, saved.getId());
        assertEquals("CLIENT", saved.getAggregateType());
        assertEquals("42", saved.getAggregateId());
        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
        assertTrue(saved.getPayload().contains("\"email\":\"kait@gym.com\""));
        assertTrue(saved.getPayload().contains("\"clientId\":42"));
        assertTrue(saved.getPayload().contains("\"eventId\":\"" + eventId + "\""));
        assertTrue(saved.getPayload().contains("\"timestamp\":\"2026-11-12T00:00:00Z\""));

    }


}