package com.sprint.smartgymcore.dto.common;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ApiErrorResponse(
        int statusCode,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public ApiErrorResponse(int statusCode, String error, String message) {
        this(statusCode, error, message, LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

    }
}
