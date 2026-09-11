package com.sprint.smartgymcore.dto.access;

import com.sprint.smartgymcore.model.AccessDirection;
import jakarta.validation.constraints.NotNull;

public record AccessCheckRequest(
        @NotNull(message = "RFID token cannot be empty")
        String rfidToken,

        @NotNull(message = "Zone ID cannot be null")
        Long zoneId,

        @NotNull(message = "Direction must be 'IN' or 'OUT'")
        AccessDirection direction
) {
}
