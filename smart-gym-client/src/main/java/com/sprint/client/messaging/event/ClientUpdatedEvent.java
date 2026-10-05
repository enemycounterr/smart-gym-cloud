package com.sprint.client.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record ClientUpdatedEvent(
        UUID eventId,
        Long clientId,
        String name,
        String email,
        Instant timestamp
) { }
