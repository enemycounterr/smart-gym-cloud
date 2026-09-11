package com.sprint.smartgymcore.dto.access;

import java.time.Instant;

public record ClientInsideResponse(
        Long clientId,
        String clientName,
        Instant insideSince
) {
}
