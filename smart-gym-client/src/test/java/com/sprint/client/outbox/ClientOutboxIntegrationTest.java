package com.sprint.client.outbox;

import com.sprint.client.BaseIntegrationTest;
import com.sprint.client.config.rabbitmq.ClientRabbitProperties;
import com.sprint.client.dto.ClientCreateRequest;
import com.sprint.client.dto.ClientResponse;
import com.sprint.client.messaging.event.ClientCreatedEvent;
import com.sprint.client.outbox.model.OutboxEvent;
import com.sprint.client.outbox.model.OutboxStatus;
import com.sprint.client.outbox.scheduler.OutboxScheduler;
import com.sprint.client.service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClientOutboxIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ClientRabbitProperties rabbitProperties;

    @Test
    @DisplayName("ClientSpecification: should filter clients case-insensitively by name or email")
    void getAllClients_shouldFilterByKeyword() {

        clientService.createClient(new ClientCreateRequest("Oleksandr Koval", "koval@gym.com"));
        clientService.createClient(new ClientCreateRequest("Maria Shevchenko", "maria@gym.com"));

        Page<ClientResponse> byName = clientService.getAllClients(
                "kOvAl",
                PageRequest.of(0, 10)
        );

        assertEquals(1, byName.getTotalElements());
        assertEquals("Oleksandr Koval", byName.getContent().getFirst().name());

        Page<ClientResponse> byEmail = clientService.getAllClients(
                "MARIA@",
                PageRequest.of(0, 10)
        );

        assertEquals(1, byEmail.getTotalElements());
        assertEquals("Maria Shevchenko", byEmail.getContent().getFirst().name());

    }

    @Test
    @DisplayName("Should atomically persist Client + PENDING OutboxEvent and relay message to RabbitMQ")
    void createClientAndRelayOutbox_shouldPersistAndPublishToRabbitMq() {

        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate);
        Queue testQueue = new AnonymousQueue();
        rabbitAdmin.declareQueue(testQueue);
        rabbitAdmin.declareBinding(
                BindingBuilder
                        .bind(testQueue)
                        .to(new DirectExchange(rabbitProperties.exchange()))
                        .with(rabbitProperties.routingKeys().created())
        );

        ClientResponse created = clientService.createClient(
                new ClientCreateRequest("Kir", "kiri@smartgym.com")
        );

        assertNotNull(created.id());

        List<OutboxEvent> pendingEvents = outboxRepository.findAll();
        assertEquals(1, pendingEvents.size());
        OutboxEvent eventBeforeRelay = pendingEvents.getFirst();
        assertEquals(OutboxStatus.PENDING, eventBeforeRelay.getStatus());
        assertEquals("ClientCreated", eventBeforeRelay.getEventType());
        assertNull(eventBeforeRelay.getSentAt());

        outboxScheduler.processOutboxEvents();

        OutboxEvent eventAfterRelay = outboxRepository.findById(eventBeforeRelay.getId()).orElseThrow();
        assertEquals(OutboxStatus.SENT, eventAfterRelay.getStatus());
        assertNotNull(eventAfterRelay.getSentAt());

        Message received = rabbitTemplate.receive(testQueue.getName(), 3000);
        assertNotNull(received, "Message must be delivered to RabbitMQ");
        assertEquals("ClientCreated", received.getMessageProperties().getHeader("eventType"));
        assertEquals(eventBeforeRelay.getId().toString(), received.getMessageProperties().getMessageId());
        assertTrue(new String(received.getBody()).contains("kiri@smartgym.com"));

    }
}
