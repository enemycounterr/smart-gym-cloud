package com.sprint.client.controller;

import com.sprint.client.dto.ClientCreateRequest;
import com.sprint.client.dto.ClientResponse;
import com.sprint.client.security.InternalApiKeyFilter;
import com.sprint.client.security.JwtService;
import com.sprint.client.security.SecurityConfig;
import com.sprint.client.service.ClientService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import(SecurityConfig.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private JwtService jwtService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("GET /api/v1/clients/{id}: should return 401 when unauthenticated")
    void getClientById_whenUnauthenticated_shouldReturn401() throws Exception {

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("GET /api/v1/clients/{id}: should allow GUARD role")
    void getClientById_whenGuardRole_shouldReturn200() throws Exception {

        when(clientService.getClientById(1L))
                .thenReturn(new ClientResponse(1L, "Greek", "greek@gym.com", true));

        mockMvc.perform(get("/api/v1/clients/1")
                        .with(user("guardUser").roles("GUARD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Greek"))
                .andExpect(jsonPath("$.email").value("greek@gym.com"));

    }

    @Test
    @DisplayName("POST /api/v1/clients: should forbid GUARD role from creating clients")
    void createClient_whenGuardRole_shouldReturn403() throws Exception {

        ClientCreateRequest request = new ClientCreateRequest("Greek", "greek@gym.com");

        mockMvc.perform(post("/api/v1/clients")
                        .with(user("guardUser").roles("GUARD"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/clients: should return 400 when email is invalid")
    void createClient_whenInvalidRequest_shouldReturn400() throws Exception {

        ClientCreateRequest invalidRequest = new ClientCreateRequest("", "not-an-email");

        mockMvc.perform(post("/api/v1/clients")
                        .with(user("adminUser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @DisplayName("POST /api/v1/clients: should return 200 and created client for ADMIN")
    void createClient_whenValidAdminRequest_shouldReturn200() throws Exception {

        ClientCreateRequest request = new ClientCreateRequest("Greek", "greek@gym.com");
        when(clientService.createClient(any(ClientCreateRequest.class)))
                .thenReturn(new ClientResponse(1L, "Greek", "greek@gym.com", true));

        mockMvc.perform(post("/api/v1/clients")
                        .with(user("adminUser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.name").value("Greek"))
                .andExpect(jsonPath("$.email").value("greek@gym.com"));

    }

    @Test
    @DisplayName("GET /api/v1/clients/1: should return 401 when Bearer token is invalid")
    void getClientById_whenInvalidToken_shouldReturn401() throws Exception {

        when(jwtService.extractAllClaims("bad-token"))
                .thenThrow(new MalformedJwtException("Invalid token"));

        mockMvc.perform(get("/api/v1/clients/1")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token is expired or invalid"));

    }

}