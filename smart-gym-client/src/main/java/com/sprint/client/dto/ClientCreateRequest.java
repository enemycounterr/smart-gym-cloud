package com.sprint.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientCreateRequest(
        @NotBlank(message = "the name cant be empty")
        String name,

        @NotBlank(message = "the email cant be empty")
        @Email(message = "incorrect format of email")
        String email
){}
