package com.sprint.notification.listener;

import com.sprint.notification.dto.AccessRegisterEvent;
import com.sprint.notification.exception.CrmIntegrationException;
import com.sprint.notification.integration.CrmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessNotificationListener {

    private final CrmClient crmClient;

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void handleAccessEvent(AccessRegisterEvent event) {
        log.info("\tRCV EVENT FROM RABBITMQ\nClient: {} (ID: {}) With timestamp: {}",
                event.clientName(), event.clientId(), event.timestamp());

        if ("IN".equals(event.direction())) {
            try {
                log.info("Sending data to external CRM");
                this.crmClient.sendLoyaltyPoints(event.clientId(), event.clientName());
                log.info("CRM successfully updated!");
            } catch (CrmIntegrationException ex) {
                log.error("CRM Integration Failed: {}", ex.getMessage());
                throw new AmqpRejectAndDontRequeueException("Routing message to DLQ due to CRM failure", ex);
            }
        }
    }
}
