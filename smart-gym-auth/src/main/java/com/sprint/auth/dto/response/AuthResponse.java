package com.sprint.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
