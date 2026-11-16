package com.sprint.auth.controller;

import com.sprint.auth.security.OAuth2LoginSuccessHandler;
import com.sprint.auth.security.SecurityConfig;
import com.sprint.auth.service.AuthService;
import com.sprint.auth.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(UserAdminController.class)
@Import(SecurityConfig.class)
class UserAdminControllerTest {

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

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("POST /api/v1/admin/users/{id}/revoke: should forbid GUARD role with 403")
    void revokeTokens_whenGuardRole_shouldReturn403() throws Exception {

        mockMvc.perform(post("/api/v1/admin/users/2/revoke")
                        .with(user("guardUser").roles("GUARD")))
                .andExpect(status().isForbidden());

    }

}