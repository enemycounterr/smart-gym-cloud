package com.sprint.notification.controller;


import com.sprint.notification.security.JwtService;
import com.sprint.notification.security.SecurityConfig;
import com.sprint.notification.service.RabbitAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitAdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-secret-key-for-testing-purposes-only-32chars")
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RabbitAdminService rabbitAdminService;

    @Test
    @DisplayName("GUARD role should get 403 on admin endpoint")
    void guardRoleShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/rabbitmq/reprocess-dlq")
                        .with(user("testuser").roles("GUARD")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN role should pass through")
    void adminRoleShouldBeAllowed() throws Exception {
        when(rabbitAdminService.reprocessDlqMessages()).thenReturn(3);

        mockMvc.perform(post("/api/v1/admin/rabbitmq/reprocess-dlq")
                        .with(user("testuser").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

}