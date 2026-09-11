package com.sprint.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank
        @Email(message = "Invalid email format")
        String email
) {
}
