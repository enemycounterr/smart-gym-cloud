package com.sprint.auth.service;


import com.sprint.auth.dto.request.LoginRequest;
import com.sprint.auth.dto.request.RegisterRequest;
import com.sprint.auth.dto.response.AuthResponse;
import com.sprint.auth.exception.UserAlreadyExistException;
import com.sprint.auth.model.RefreshToken;
import com.sprint.auth.model.Role;
import com.sprint.auth.model.User;
import com.sprint.auth.repository.RefreshTokenRepository;
import com.sprint.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {

        authService = new AuthService(
                userRepository,
                jwtService,
                authenticationManager,
                passwordEncoder,
                refreshTokenRepository
        );
        ReflectionTestUtils.setField(authService, "refreshExpirationDays", 7L);

    }

    @Test
    @DisplayName("register: should encode password, save user, and return access + refresh tokens")
    void register_whenDataIsUnique_shouldSaveUserAndReturnTokens() {

        RegisterRequest request = new RegisterRequest(
                "alex",
                "rawPass123",
                "guard",
                "alex@gym.com"
        );
        when(userRepository.existsByUsername("alex")).thenReturn(false);
        when(userRepository.existsByEmail("alex@gym.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPass123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("access-jwt");

        AuthResponse response = authService.register(request);

        assertEquals("access-jwt", response.accessToken());
        assertNotNull(response.refreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hashedPass", userCaptor.getValue().getPassword());
        assertEquals(Role.GUARD, userCaptor.getValue().getRole());
        verify(refreshTokenRepository).save(any(RefreshToken.class));

    }

    @Test
    @DisplayName("register: should throw UserAlreadyExistException when username or email is taken")
    void register_whenUsernameExists_shouldThrowException() {

        RegisterRequest request = new RegisterRequest("alex", "rawPass123", "GUARD", "alex@gym.com");
        when(userRepository.existsByUsername("alex")).thenReturn(true);

        assertThrows(UserAlreadyExistException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());

    }

    @Test
    @DisplayName("login: should revoke previous refresh tokens and return new token pair")
    void login_whenCredentialsAreValid_shouldReturnAuthResponse() {

        LoginRequest request = new LoginRequest("danek", "admin");
        User user = new User("danek", "encoded", Role.ADMIN);
        user.setId(1L);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(user)).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.accessToken());
        assertNotNull(response.refreshToken());
        verify(refreshTokenRepository).deleteAllByUserId(1L);
    }

    @Test
    public void login_whenAuthenticationFails_shouldPropagateBadCredentials() {

        LoginRequest request = new LoginRequest("unknown", "password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request));

    }


}
