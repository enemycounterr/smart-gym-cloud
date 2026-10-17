package com.sprint.smartgymcore.messaging.outbox.service;


import com.sprint.smartgymcore.config.rabbitmq.notification.NotificationRabbitProperties;
import com.sprint.smartgymcore.messaging.event.outbound.AccessRegisterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxAppender {

    private final OutboxService outboxService;
    private final NotificationRabbitProperties notificationProps;

    /**
         Bridges domain events and the Outbox pattern, keeping AccessService free of messaging concerns.

         AccessService publishes an AccessRegisterEvent via ApplicationEventPublisher after a successful
         turnstile registration. This class intercepts that event and delegates persistence to OutboxService,
         which stores it as a PENDING outbox entry for reliable delivery to RabbitMQ.

         Flow: AccessService → AccessRegisterEvent → OutboxAppender → OutboxService → outbox_events table
     */

    @EventListener
    public void handleAccessRegisterEvent(AccessRegisterEvent event) {
        log.debug("Intercepted AccessRegisterEvent for logId: {}. Delegating to OutboxService.", event.logId());

        outboxService.saveEvent(
                UUID.randomUUID(),
                "ACCESS_LOG",
                String.valueOf(event.logId()),
                "ACCESS_REGISTERED",
                notificationProps.routingKeys().accessRegistered(),
                event
        );
    }

}
