package com.sprint.client.outbox.scheduler;

import com.sprint.client.config.rabbitmq.ClientRabbitProperties;
import com.sprint.client.outbox.model.OutboxEvent;
import com.sprint.client.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final RabbitTemplate rabbitTemplate;
    private final ClientRabbitProperties clientRabbitProperties;
    private final OutboxService outboxService;

    /*
     * Scheduler independently polls pending outbox events and publishes them to RabbitMQ.
     */
    @Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay:2000}")
    public void processOutboxEvents() {

        List<OutboxEvent> pendingEvents = outboxService.findPendingEvents();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events. Starting relay to RabbitMQ", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                sendEvent(event);
            } catch (AmqpException e) {
                log.error("Failed to relay outbox event [{}] id={}. Stopping current batch.",
                        event.getEventType(), event.getId(), e);

                outboxService.handleFailure(event, e);
                break;
            } catch (Exception e) {
                log.error("Unexpected error processing event id={}", event.getId(), e);
                outboxService.handleFailure(event, e);
            }
        }

    }

    private void sendEvent(OutboxEvent event) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setMessageId(event.getId().toString());
        properties.setHeader("eventType", event.getEventType());
        properties.setHeader("aggregateType", event.getAggregateType());

        Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), properties);

        rabbitTemplate.send(
                clientRabbitProperties.exchange(),
                event.getRoutingKey(),
                message
        );

        outboxService.markAsSent(event);
        log.info("Successfully relayed outbox event [{}] id={} to exchange=[{}] routingKey=[{}]",
                event.getEventType(), event.getId(), clientRabbitProperties.exchange(), event.getRoutingKey());
    }


}
