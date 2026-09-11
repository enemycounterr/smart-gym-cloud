package com.sprint.smartgymcore.external.client;

public record ClientResponse(
        Long id,
        String name,
        String email,
        boolean isActive
) {
}
