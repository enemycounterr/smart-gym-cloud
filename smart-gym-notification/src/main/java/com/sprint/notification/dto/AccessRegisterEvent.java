package com.sprint.notification.dto;

import java.time.Instant;

public record AccessRegisterEvent(
        Long logId,
        Long clientId,
        String clientName,
        String zoneName,
        String direction,
        Instant timestamp
) { }
