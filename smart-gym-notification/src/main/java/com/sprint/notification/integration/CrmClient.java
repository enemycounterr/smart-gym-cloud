package com.sprint.notification.integration;


import com.sprint.notification.exception.CrmIntegrationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
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

    @Retry(name = "crmService")
    @CircuitBreaker(name = "crmService", fallbackMethod = "sendLoyaltyPointsFallback")
    public void sendLoyaltyPoints(Long clientId, String clientName) {
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
    }

    public void sendLoyaltyPointsFallback(Long clientId, String clientName, Throwable t) {
        log.error("Circuit Breaker [crmService] triggered for clientId: {} ({}). Reason: {}",
                clientId, clientName, t.getMessage());

        throw new CrmIntegrationException("CRM integration failed (Circuit Breaker): " + t.getMessage(), t);
    }
}
