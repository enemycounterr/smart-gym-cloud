package com.sprint.smartgymcore.messaging.outbox.service;

import com.sprint.smartgymcore.config.rabbitmq.notification.NotificationRabbitProperties;
import com.sprint.smartgymcore.messaging.event.outbound.AccessRegisterEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxAppenderTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private NotificationRabbitProperties notificationProps;

    @InjectMocks
    private OutboxAppender outboxAppender;

    @Test
    @DisplayName("Should intercept AccessRegisterEvent and delegate to OutboxService")
    public void handleAccessRegisteredEvent_shouldSaveToOutbox() {
        // Arrange
        NotificationRabbitProperties.RoutingKeys routingKeys = mock(NotificationRabbitProperties.RoutingKeys.class);
        when(notificationProps.routingKeys()).thenReturn(routingKeys);
        when(routingKeys.accessRegistered()).thenReturn("gym.access.registered");

        AccessRegisterEvent event = new AccessRegisterEvent(
                100L, 1L, "Danek", "GYM", "IN", Instant.now()
        );

        // Act
        outboxAppender.handleAccessRegisterEvent(event);

        // Assert
        verify(outboxService).saveEvent(
                any(UUID.class),
                eq("ACCESS_LOG"),
                eq("100"),
                eq("ACCESS_REGISTERED"),
                eq("gym.access.registered"),
                eq(event)
        );
        verifyNoMoreInteractions(outboxService);
    }
}
