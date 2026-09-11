package com.sprint.auth.dto.response;

public record UserResponse(
        Long id,
        String userName,
        String email,
        String role
) {
}
