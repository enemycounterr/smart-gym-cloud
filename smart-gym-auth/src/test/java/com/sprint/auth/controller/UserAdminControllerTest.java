package com.sprint.auth.controller;

import com.sprint.auth.dto.request.UpdateRoleRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
    import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

    @Test
    @DisplayName("POST /api/v1/admin/users/{id}/revoke: should allow ADMIN role and return 204")
    void revokeTokens_whenAdminRole_shouldReturn204() throws Exception {

        mockMvc.perform(post("/api/v1/admin/users/2/revoke")
                        .with(user("adminUser").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(authService).revokeAllUserTokens(2L);

    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role: should update role for ADMIN")
    void updateRole_whenAdminRole_shouldReturn204() throws Exception {

        UpdateRoleRequest request = new UpdateRoleRequest("ADMIN");

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .with(user("adminUser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).updateRole(2L, request);

    }

    @Test
    @DisplayName("DELETE /api/v1/admin/users/{id}: should pass current User entity to service")
    void deleteUser_whenAdminRole_shouldReturn204() throws Exception {

        User admin = new User("adminUser", "hash", Role.ADMIN);
        admin.setId(1L);
        var auth = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());

        mockMvc.perform(delete("/api/v1/admin/users/2")
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        verify(authService).deleteUser(2L, admin);

    }

}