package com.sprint.smartgymcore.external.client;

import com.sprint.smartgymcore.exceptions.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Set;


/*
    CLASS ADAPTER FOR ClientApiClient
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientExternalService {

    private final ClientApiClient clientApiClient;

    @Retry(name = "clientService")
    @CircuitBreaker(name = "clientService", fallbackMethod = "getClientByIdFallback")
    public ClientResponse getClientById(Long id) {
        try {
            return clientApiClient.getClientById(id);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Client not found with ID: " + id);
            }
            throw e;
        }
    }

    @CircuitBreaker(name = "clientService", fallbackMethod = "getClientsByIdsFallback")
    public List<ClientResponse> getClientsByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return clientApiClient.getClientsByIds(ids);
    }

    public ClientResponse getClientByIdFallback(Long id, Throwable t) {
        if (t instanceof ResourceNotFoundException ex) {
            throw ex;
        }

        log.error("Circuit Breaker [clientService] triggered for getClientById({}). Reason: {}", id, t.
                getMessage());
        throw new IllegalStateException("Client Service is temporarily unavailable. Please try again later.", t);
    }

    public List<ClientResponse> getClientsByIdsFallback(Set<Long> ids, Throwable t) {
        log.error("Circuit Breaker [clientService] triggered for getClientsByIds. Returning empty fallback list.Reason: {}", t.getMessage());
        return List.of();
    }

}
