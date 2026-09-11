package com.sprint.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
        @NotBlank(message = "Role cant be empty")
        String role
) {
}
