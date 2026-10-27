package com.sprint.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityInegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @DisplayName("Should return 403 Forbidden when calling /internal/** endpoint")
    void shouldReturnForbiddenForInternalEndpoints() {
        webTestClient.get()
                .uri("/internal/v1/clients/1")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should return 403 Forbidden when calling nested /*/internal/** endpoint")
    void shouldReturnForbiddenForNestedInternalEndpoints() {
        webTestClient.get()
                .uri("/v1/internal/clients/1")
                .exchange()
                .expectStatus().isForbidden();
    }

}