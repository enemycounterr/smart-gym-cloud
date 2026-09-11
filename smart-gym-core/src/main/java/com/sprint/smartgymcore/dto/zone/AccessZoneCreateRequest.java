package com.sprint.smartgymcore.dto.zone;

import jakarta.validation.constraints.NotBlank;

public record AccessZoneCreateRequest(
        @NotBlank(message = "Zone name cant be empty")
        String zoneName
) {
}
