package com.sprint.smartgymcore.external.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.sprint.smartgymcore.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DirtiesContext
public class ClientExternalServiceWireMockTest extends BaseIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private ClientExternalService clientExternalService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("application.client-service.url", wireMock::baseUrl);
    }

    @Test
    @DisplayName("Retry: should repeat request when 500 status code and return result if success")
    void getClientById_shouldRetryAndSucceed() {

        String scenarioName = "Retry Scenario";

        wireMock.stubFor(get(urlEqualTo("/internal/v1/clients/1"))
                .inScenario(scenarioName)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("SECOND_ATTEMPT"));

        wireMock.stubFor(get(urlEqualTo("/internal/v1/clients/1"))
                .inScenario(scenarioName)
                .whenScenarioStateIs("SECOND_ATTEMPT")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": 1,
                                    "name": "Alex",
                                    "email": "alex@test.com",
                                    "isActive": true
                                }
                                """)
                        .withStatus(200)));

        ClientResponse response = clientExternalService.getClientById(1L);

        assertNotNull(response);
        assertEquals("Alex", response.name());

        wireMock.verify(2, getRequestedFor(urlEqualTo("/internal/v1/clients/1")));
    }
}
