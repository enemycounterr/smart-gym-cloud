package com.sprint.smartgymcore.dto.access;

import com.sprint.smartgymcore.model.AccessDirection;

import java.time.Instant;

public record AccessLogResponse(
        Long logId,
        Long clientId,
        String clientName,
        String zoneName,
        AccessDirection direction,
        Instant timestamp
) {
}
