package com.sprint.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank(message = "Username cannot be empty")
        String username,

        @NotBlank(message = "Password cannot be empty")
        String password,

        @NotBlank(message = "Role cannot be empty (ADMIN or GUARD)")
        @Pattern(
                regexp = "^(?i)(ADMIN|GUARD)$",
                message = "Role must be either ADMIN or GUARD"
        )
        String role,

        @Email(message = "Invalid email type")
        String email
) {
}
