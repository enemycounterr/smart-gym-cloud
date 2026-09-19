package com.sprint.smartgymcore.messaging.publisher;


import com.sprint.smartgymcore.config.rabbitmq.notification.NotificationRabbitProperties;
import com.sprint.smartgymcore.messaging.event.outbound.AccessRegisterEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessOutboundEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private NotificationRabbitProperties notificationRabbitProperties;

    @InjectMocks
    private AccessOutboundEventPublisher publisher;

    @Test
    @DisplayName("Should route AccessRegisterEvent to RabbitMQ with correct exchange and routing key")
    public void handleAccessRegisteredEvent_shouldSendToRabbitMQ() {
        // Arrange
        NotificationRabbitProperties.RoutingKeys routingKeys = mock(NotificationRabbitProperties.RoutingKeys.class);
        when(notificationRabbitProperties.exchange()).thenReturn("gym.events");
        when(notificationRabbitProperties.routingKeys()).thenReturn(routingKeys);
        when(routingKeys.accessRegistered()).thenReturn("gym.access.registered");

        AccessRegisterEvent event = new AccessRegisterEvent(
                100L, 1L, "Danek", "GYM", "IN", Instant.now()
        );

        // Act
        publisher.handleAccessRegisteredEvent(event);

        // Assert
        verify(rabbitTemplate).convertAndSend("gym.events", "gym.access.registered", event);
        verifyNoMoreInteractions(rabbitTemplate);
    }

}
