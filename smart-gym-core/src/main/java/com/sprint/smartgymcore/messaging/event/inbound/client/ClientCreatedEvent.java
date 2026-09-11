package com.sprint.smartgymcore.messaging.event.inbound.client;

public record ClientCreatedEvent(
        Long clientId,
        String name,
        String email
) {
}
