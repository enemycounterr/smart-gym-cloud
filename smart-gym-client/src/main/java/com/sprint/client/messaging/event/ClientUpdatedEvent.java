package com.sprint.client.messaging.event;

public record ClientUpdatedEvent(
        Long clientId,
        String name,
        String email
) { }
