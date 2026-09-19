package com.sprint.smartgymcore.messaging.publisher;

import com.sprint.smartgymcore.config.rabbitmq.notification.NotificationRabbitProperties;
import com.sprint.smartgymcore.messaging.event.outbound.AccessRegisterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessOutboundEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final NotificationRabbitProperties notificationRabbitProperties;

    /**
     * This method only fires AFTER the database transaction commits successfully.
     * The parameter type (AccessRegisterEvent) is how Spring knows to route the event here.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAccessRegisteredEvent(AccessRegisterEvent event) {
        log.info("Transaction committed successfully. Dispatching event to RabbitMQ for logId: {}", event.logId());

        this.rabbitTemplate.convertAndSend(
                notificationRabbitProperties.exchange(),
                notificationRabbitProperties.routingKeys().accessRegistered(),
                event
        );
    }
}
