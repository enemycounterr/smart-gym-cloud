package com.sprint.smartgymcore.messaging.event.inbound.client;

public record ClientStatusChangedEvent(
        Long clientId,
        boolean isActive
) {
}
