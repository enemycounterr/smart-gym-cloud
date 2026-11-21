package com.sprint.client.controller;

import com.sprint.client.dto.ClientResponse;
import com.sprint.client.security.InternalApiKeyFilter;
import com.sprint.client.security.JwtAuthenticationFilter;
import com.sprint.client.security.JwtService;
import com.sprint.client.security.SecurityConfig;
import com.sprint.client.service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalClientController.class)
@Import({SecurityConfig.class, InternalApiKeyFilter.class})
@ActiveProfiles("test")
class InternalClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private JwtService jwtService;

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String VALID_KEY = "test-internal-secret-key-12345";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("GET /internal/v1/clients/{id}: should return 401 when header is missing")
    void getClientById_whenHeaderMissing_shouldReturn401() throws Exception {

        mockMvc.perform(get("/internal/v1/clients/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

    }

    @Test
    @DisplayName("GET /internal/v1/clients/{id}: should return 401 when header is invalid")
    void getClientById_whenHeaderInvalid_shouldReturn401() throws Exception {

        mockMvc.perform(get("/internal/v1/clients/1")
                        .header(API_KEY_HEADER, "wrong-secret-key"))
                .andExpect(status().isUnauthorized());

    }

    @Test
    @DisplayName("GET /internal/v1/clients/{id}: should return 200 when header is valid")
    void getClientById_whenHeaderValid_shouldReturn200() throws Exception {

        when(clientService.getClientById(1L))
                .thenReturn(new ClientResponse(1L, "Filthy", "filthy@gym.com", true));

        mockMvc.perform(get("/internal/v1/clients/1")
                        .header(API_KEY_HEADER, VALID_KEY)
                        .with(user("INTERNAL_SERVICE").authorities(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Filthy"))
                .andExpect(jsonPath("$.email").value("filthy@gym.com"));
    }

    @Test
    @DisplayName("POST /internal/v1/clients/batch: should return 200 with clients when header is valid")
    void getClientsBatch_whenHeaderValid_shouldReturn200() throws Exception {

        when(clientService.getClientsByIds(Set.of(1L)))
                .thenReturn(List.of(new ClientResponse(1L, "Filthy", "filthy@gym.com", true)));

        mockMvc.perform(post("/internal/v1/clients/batch")
                        .header(API_KEY_HEADER, VALID_KEY)
                        .with(user("INTERNAL_SERVICE").authorities(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Set.of(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Filthy"));

    }
}