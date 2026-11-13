package com.sprint.client.service;

import com.sprint.client.config.rabbitmq.ClientRabbitProperties;
import com.sprint.client.dto.ClientCreateRequest;
import com.sprint.client.dto.ClientResponse;
import com.sprint.client.dto.ClientUpdateRequest;
import com.sprint.client.exception.ClientAlreadyExistException;
import com.sprint.client.exception.ResourceNotFoundException;
import com.sprint.client.mapper.ClientMapper;
import com.sprint.client.messaging.event.ClientCreatedEvent;
import com.sprint.client.messaging.event.ClientStatusChangedEvent;
import com.sprint.client.messaging.event.ClientUpdatedEvent;
import com.sprint.client.model.Client;
import com.sprint.client.outbox.service.OutboxService;
import com.sprint.client.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    private final ClientMapper clientMapper = new ClientMapper();

    private final ClientRabbitProperties rabbitProperties = new ClientRabbitProperties(
            "client.direct",
            new ClientRabbitProperties.RoutingKeys(
                    "client.created",
                    "client.status",
                    "client.updated"
            )
    );

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private OutboxService outboxService;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository, clientMapper, outboxService, rabbitProperties);
    }

    @Test
    @DisplayName("createClient: should save client and append ClientCreatedEvent to outbox")
    void createClient_whenEmailIsUnique_shouldSaveClientAndOutboxEvent() {

        ClientCreateRequest request = new ClientCreateRequest("Ivan", "ivan@gym.com");
        Client savedClient = Client.builder()
                .id(1L)
                .name("Ivan")
                .email("ivan@gym.com")
                .isActive(true)
                .build();

        when(clientRepository.existsByEmail("ivan@gym.com")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        ClientResponse response = clientService.createClient(request);

        assertEquals(1L, response.id());
        assertEquals("Ivan", response.name());
        assertTrue(response.isActive());

        verify(outboxService).saveEvent(
                any(UUID.class),
                eq("CLIENT"),
                eq("1"),
                eq("ClientCreated"),
                eq("client.created"),
                any(ClientCreatedEvent.class)
        );

    }

    @Test
    @DisplayName("createClient: should throw ClientAlreadyExistException when email is taken")
    void createClient_whenEmailExists_shouldThrowException() {

        ClientCreateRequest request = new ClientCreateRequest("Ivan", "ivan@gym.com");
        when(clientRepository.existsByEmail("ivan@gym.com")).thenReturn(true);

        assertThrows(ClientAlreadyExistException.class, () -> clientService.createClient(request));

        verify(clientRepository, never()).save(any());
        verifyNoInteractions(outboxService);

    }

    @Test
    @DisplayName("updateClient: should not save outbox event if fields were not modified")
    void updateClient_whenDataUnchanged_shouldNotSaveOutboxEvent() {

        Client existing = Client.builder().id(1L).name("Ivan").email("ivan@gym.com").isActive(true).build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        ClientUpdateRequest request = new ClientUpdateRequest("ivan", "IVAN@gym.com");
        ClientResponse response = clientService.updateClient(1L, request);

        assertEquals("Ivan", response.name());
        verifyNoInteractions(outboxService);

    }

    @Test
    @DisplayName("updateClient: should update fields and save ClientUpdatedEvent when modified")
    void updateClient_whenDataChanged_shouldSaveOutboxEvent() {

        Client existing = Client.builder()
                .id(1L)
                .name("Ivan")
                .email("ivan@gym.com")
                .isActive(true)
                .build();
        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.existsByEmail("new@gym.com")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse response = clientService.updateClient(1L, new ClientUpdateRequest(
                "Changed",
                "new@gym.com")
        );

        assertEquals("Changed", response.name());
        assertEquals("new@gym.com", response.email());

        verify(outboxService).saveEvent(
                any(UUID.class),
                eq("CLIENT"),
                eq("1"),
                eq("ClientUpdatedEvent"),
                eq("client.updated"),
                any(ClientUpdatedEvent.class)
        );

    }

    @Test
    @DisplayName("toggleClientStatus: should throw IllegalStateException if client already has target status")
    void toggleClientStatus_whenSameStatus_shouldThrowIllegalStateException() {

        Client existing = Client.builder()
                .id(1L)
                .name("Ivan")
                .email("ivan@gym.com")
                .isActive(true)
                .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> clientService.toggleClientStatus(1L, true));
        verify(clientRepository, never()).save(any());
        verifyNoInteractions(outboxService);

    }

    @Test
    @DisplayName("toggleClientStatus: should toggle active flag and save ClientStatusChangedEvent")
    void toggleClientStatus_whenStatusDiffers_shouldSaveOutboxEvent() {

        Client existing = Client.builder()
                .id(1L)
                .name("Ivan")
                .email("ivan@gym.com")
                .isActive(true)
                .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse response = clientService.toggleClientStatus(1L, false);

        assertFalse(response.isActive());
        verify(outboxService).saveEvent(
                any(UUID.class),
                eq("CLIENT"),
                eq("1"),
                eq("ClientStatusChangedEvent"),
                eq("client.status"),
                any(ClientStatusChangedEvent.class)
        );

    }

    @Test
    @DisplayName("getClientsByIds: should return empty list immediately without DB query when set is empty")
    void getClientsByIds_whenEmpty_shouldReturnEmptyList() {

        List<ClientResponse> result = clientService.getClientsByIds(Set.of());
        assertTrue(result.isEmpty());
        verifyNoInteractions(clientRepository);

    }

    @Test
    @DisplayName("getClientById: should throw ResourceNotFoundException when missing")
    void getClientById_whenNotFound_shouldThrowException() {

        when(clientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> clientService.getClientById(99L));

    }
}