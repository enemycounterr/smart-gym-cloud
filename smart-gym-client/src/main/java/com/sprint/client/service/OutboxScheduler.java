package com.sprint.client.service;

import com.sprint.client.config.RabbitMqProperties;
import com.sprint.client.model.OutboxEvent;
import com.sprint.client.model.OutboxStatus;
import com.sprint.client.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;

    private static final int MAX_RETRY_COUNT = 5;


    /*
     * Scheduler independently polls pending outbox events and publishes them to RabbitMQ.
     */
    @Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay:2000}")
    public void processOutboxEvents(){
        List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()){
            return;
        }

        log.info("Found {} pending outbox events. Starting relay to RabbitMQ", pendingEvents.size());

        for(OutboxEvent event : pendingEvents){
            try{
                sendEvent(event);
            }catch (Exception e){
                log.error("Failed to relay outbox event [{}] id={}. Stopping current batch.",
                        event.getEventType(), event.getId(), e);
                handleFailure(event, e);
                break;
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
                rabbitMqProperties.exchange(),
                event.getRoutingKey(),
                message
        );

        markAsSent(event);
        log.info("Successfully relayed outbox event [{}] id={} to exchange=[{}] routingKey=[{}]",
                event.getEventType(), event.getId(), rabbitMqProperties.exchange(), event.getRoutingKey());
    }

    @Transactional
    public void markAsSent(OutboxEvent event) {
        event.setStatus(OutboxStatus.SENT);
        event.setSentAt(Instant.now());
        outboxRepository.save(event);
    }

    @Transactional
    public void handleFailure(OutboxEvent event, Exception e) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(e.getMessage());

        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            event.setStatus(OutboxStatus.FAILED);
            log.error("Outbox event id={} marked as FAILED after {} attempts", event.getId(), MAX_RETRY_COUNT);
        }

        outboxRepository.save(event);
    }
}
