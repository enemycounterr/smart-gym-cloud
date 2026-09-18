package com.sprint.smartgymcore.controller;

import com.sprint.smartgymcore.security.JwtAuthenticationFilter;
import com.sprint.smartgymcore.security.JwtService;
import com.sprint.smartgymcore.service.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccessController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AccessControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AccessService accessService;

    @Test
    void registerAccess_shouldAcceptLowerCaseEnum() throws Exception {
        String jsonRequest = """
                {
                    "rfidToken": "RFID-12345",
                    "zoneId": 1,
                    "direction": "out"
                }
                """;

        mockMvc.perform(post("/api/v1/access/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk()); // или isCreated(), в зависимости от контроллера
    }

    @Test
    void registerAccess_whenInvalidEnum_shouldReturn400BadRequest() throws Exception {
        String invalidJson = """
                    {
                        "rfidToken": "RFID-12345",
                        "zoneId": 1,
                        "direction": "SIDEWAYS"
                    }
                    """;

        mockMvc.perform(post("/api/v1/access/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Direction must be 'IN' or 'OUT'"));
    }
}
