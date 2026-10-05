package com.sprint.smartgymcore.messaging.event.inbound.client;

import java.time.Instant;
import java.util.UUID;

public record ClientCreatedEvent(
        UUID eventId,
        Long clientId,
        String name,
        String email,
        Instant timestamp
) {
}
