package com.sprint.notification.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RabbitAdminService {
    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.dlq}")
    private String dlqName;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public int reprocessDlqMessages() {
        int reprocessedCount = 0;
        Message message;

        while ((message = rabbitTemplate.receive(dlqName)) != null) {
            rabbitTemplate.send(exchangeName, routingKey, message);
            reprocessedCount++;
        }

        log.info("Reprocessed {} messages from DLQ", reprocessedCount);
        return reprocessedCount;
    }
}
