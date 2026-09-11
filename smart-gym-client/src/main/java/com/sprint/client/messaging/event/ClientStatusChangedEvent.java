package com.sprint.client.messaging.event;

public record ClientStatusChangedEvent(
        Long clientId,
        boolean isActive
) {
}
