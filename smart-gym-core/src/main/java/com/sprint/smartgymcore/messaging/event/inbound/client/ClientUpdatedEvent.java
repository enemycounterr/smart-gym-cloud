package com.sprint.smartgymcore.messaging.event.inbound.client;

public record ClientUpdatedEvent(
        Long clientId,
        String name,
        String email
) { }
