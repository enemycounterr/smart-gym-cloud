package com.sprint.client.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record ClientStatusChangedEvent(
        UUID eventId,
        Long clientId,
        boolean isActive,
        Instant timestamp
) {
}
