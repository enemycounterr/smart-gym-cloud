package com.sprint.client.messaging.event;

public record ClientCreatedEvent(
        Long clientId,
        String name,
        String email
) {
}
