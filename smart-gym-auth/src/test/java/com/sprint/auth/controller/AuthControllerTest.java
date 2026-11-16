package com.sprint.auth.controller;

import com.sprint.auth.dto.request.LoginRequest;
import com.sprint.auth.dto.request.RegisterRequest;
import com.sprint.auth.dto.response.AuthResponse;
import com.sprint.auth.exception.UserAlreadyExistException;
import com.sprint.auth.model.Role;
import com.sprint.auth.model.User;
import com.sprint.auth.security.OAuth2LoginSuccessHandler;
import com.sprint.auth.security.SecurityConfig;
import com.sprint.auth.service.AuthService;
import com.sprint.auth.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Test
    @DisplayName("POST /api/v1/auth/register: should allow anonymous access and return tokens")
    void register_whenValid_shouldReturn200AndTokens() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "guard1",
                "pass123",
                "GUARD",
                "guard1@gym.com"
        );
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

    }

    @Test
    @DisplayName("POST /api/v1/auth/register: should return 400 when role is invalid")
    void register_whenRoleInvalid_shouldReturn400() throws Exception {

        RegisterRequest invalidRequest = new RegisterRequest(
                "user1",
                "pass123",
                "SUPERUSER",
                "u@gym.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));

    }

    @Test
    @DisplayName("POST /api/v1/auth/register: should return 409 when user already exists")
    void register_whenDuplicateUser_shouldReturn409() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "danek",
                "pass123",
                "ADMIN",
                "danek@gym.com"
        );
        when(authService.register(any())).thenThrow(new UserAlreadyExistException("User already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));

    }

    @Test
    @DisplayName("POST /api/v1/auth/login: should return 401 when credentials are bad")
    void login_whenBadCredentials_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("danek", "wrong");
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect username or password"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me: should return 401 when unauthenticated")
    void getCurrentUser_whenUnauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me: should return current user profile when authenticated")
    void getCurrentUser_whenAuthenticated_shouldReturnUserResponse() throws Exception {
        User principal = new User("danek", "encoded", Role.ADMIN);
        principal.setId(1L);
        principal.setEmail("danek@gym.com");

        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("danek"))
                .andExpect(jsonPath("$.email").value("danek@gym.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

}