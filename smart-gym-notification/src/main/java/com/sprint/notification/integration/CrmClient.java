package com.sprint.notification.integration;


import com.sprint.notification.exception.CrmIntegrationException;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class CrmClient {

    private final RestClient restClient;

    public CrmClient(@Value("${integration.crm.base-url}") String baseUrl, ObservationRegistry observationRegistry) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .observationRegistry(observationRegistry)
                .requestFactory(requestFactory)
                .build();
    }

    public void sendLoyaltyPoints(Long clientId, String clientName) {
        try {
            this.restClient.post()
                    .uri("/post")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {
                                "clientId": %d,
                                "name": "%s",
                                "points": 10
                            }
                            """.formatted(clientId, clientName))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException e) {
            String errorType = (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout"))
                    ? "TIMEOUT"
                    : "REST_CLIENT_ERROR";

            throw new CrmIntegrationException("Failed to send data to CRM: " + e.getMessage());
        }
    }
}
