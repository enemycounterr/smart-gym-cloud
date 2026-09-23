package com.sprint.smartgymcore.messaging.listener;

import com.sprint.smartgymcore.messaging.event.inbound.client.ClientCreatedEvent;
import com.sprint.smartgymcore.messaging.event.inbound.client.ClientStatusChangedEvent;
import com.sprint.smartgymcore.messaging.event.inbound.client.ClientUpdatedEvent;
import com.sprint.smartgymcore.service.AccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientEventListener {
    private final AccessService accessService;

    @RabbitListener(queues = "${app.rabbitmq.client.queues.client-created}")
    public void handleClientCreated(ClientCreatedEvent event) {
        log.info("RabbitMQ [INBOUND]: Caught ClientCreatedEvent for clientId={}", event.clientId());

        try {
            this.accessService.processClientCreated(event);
        } catch (Exception e) {
            log.error("Failed to process ClientCreatedEvent for clientId: {}", event.clientId(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.client.queues.client-status}")
    public void handleClientStatusChanged(ClientStatusChangedEvent event) {
        log.info("RabbitMQ [INBOUND]: Caught ClientStatusChangedEvent for clientId={}, isActive={}",
                event.clientId(), event.isActive());
        try {
            accessService.processClientStatusChanged(event);
        } catch (Exception e) {
            log.error("Failed to process ClientStatusChangedEvent for clientId: {}", event.clientId(), e);
            throw e;
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.client.queues.client-updated}")
    public void handleClientUpdated(ClientUpdatedEvent event) {
        log.info("RabbitMQ [INBOUND]: Caught ClientUpdatedEvent for clientId={}, name={}",
                event.clientId(), event.name());
        try {
            accessService.processClientUpdated(event);
        } catch (Exception e) {
            log.error("Failed to process ClientUpdatedEvent for clientId: {}", event.clientId(), e);
            throw e;
        }
    }
}
