package com.sprint.auth.service;

import com.sprint.auth.BaseIntegrationTest;
import com.sprint.auth.dto.request.LoginRequest;
import com.sprint.auth.dto.request.RefreshRequest;
import com.sprint.auth.dto.request.RegisterRequest;
import com.sprint.auth.dto.response.AuthResponse;
import com.sprint.auth.model.RefreshToken;
import com.sprint.auth.model.Role;
import com.sprint.auth.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenCleanupService tokenCleanupService;

    @Test
    @DisplayName("Full lifecycle: register -> login -> rotate refresh token -> block replay attack")
    void authLifecycle_shouldRotateTokensAndTerminateSessionsOnReuse() {

        AuthResponse registered = authService.register(
                new RegisterRequest("danek", "secret123", "ADMIN", "danek@smartgym.com")
        );
        assertEquals("danek", jwtService.extractAllClaims(registered.accessToken()).getSubject());

        AuthResponse loggedIn = authService.login(new LoginRequest("danek@smartgym.com", "secret123"));
        assertEquals(1, refreshTokenRepository.count());

        // Rotate refresh token
        AuthResponse rotated = authService.refresh(new RefreshRequest(loggedIn.refreshToken()));
        assertNotEquals(loggedIn.refreshToken(), rotated.refreshToken());
        assertEquals(2, refreshTokenRepository.count());


        assertThrows(AccessDeniedException.class,
                () -> authService.refresh(new RefreshRequest(loggedIn.refreshToken())));

        assertEquals(0, refreshTokenRepository.count());

    }

    @Test
    @DisplayName("UserDetailsService: should resolve user by both username and email")
    void userDetailsService_shouldLoadByUsernameOrEmail() {

        authService.register(new RegisterRequest(
                "guard_ivan",
                "pass123",
                "GUARD",
                "ivan@smartgym.com")
        );

        assertEquals("guard_ivan", userDetailsService.loadUserByUsername("guard_ivan").getUsername());
        assertEquals("guard_ivan", userDetailsService.loadUserByUsername("ivan@smartgym.com").getUsername());

    }

    @Test
    @DisplayName("TokenCleanupService: should delete only expired refresh tokens from DB")
    void tokenCleanupService_shouldRemoveOnlyExpiredTokens() {

        User user = userRepository.save(new User("cleanup_user", "hash", Role.GUARD));

        refreshTokenRepository.save(new RefreshToken("expired-1", user, LocalDateTime.now().minusHours(2)));
        refreshTokenRepository.save(new RefreshToken("active-1", user, LocalDateTime.now().plusDays(3)));

        tokenCleanupService.cleanExpiredRefreshTokens();

        assertEquals(1, refreshTokenRepository.count());
        assertTrue(refreshTokenRepository.findByToken("active-1").isPresent());
        assertTrue(refreshTokenRepository.findByToken("expired-1").isEmpty());

    }
}
