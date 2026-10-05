package com.sprint.smartgymcore.messaging.event.inbound.client;

import java.time.Instant;
import java.util.UUID;

public record ClientStatusChangedEvent(
        UUID eventId,
        Long clientId,
        boolean isActive,
        Instant timestamp
) {
}
