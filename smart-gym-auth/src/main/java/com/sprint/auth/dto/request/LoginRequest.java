package com.sprint.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username or Email cant be empty")
        String username,

        @NotBlank(message = "Pasword cant be empty")
        String password
) {
}
