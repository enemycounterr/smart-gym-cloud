package com.sprint.client.service;


import com.sprint.client.config.RabbitMqProperties;
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
import com.sprint.client.repository.ClientRepository;
import com.sprint.client.specification.ClientSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final RabbitTemplate rabbitTemplate;

    private final RabbitMqProperties rabbitMqProperties;

    @Transactional
    public ClientResponse createClient(ClientCreateRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new ClientAlreadyExistException("Client with email " + request.email() + " already exist");
        }

        Client newClient = clientMapper.toEntity(request);
        Client savedClient = clientRepository.save(newClient);

        ClientCreatedEvent event = new ClientCreatedEvent(
                savedClient.getId(),
                savedClient.getName(),
                savedClient.getEmail()
        );

        this.rabbitTemplate.convertAndSend(
                rabbitMqProperties.exchange(),
                rabbitMqProperties.routingKeys().created(),
                event
        );

        return clientMapper.toDto(savedClient);
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> getAllClients(String searchKeyword, Pageable pageable) {
        Specification<Client> spec = ClientSpecification.searchByKeyword(searchKeyword);

        Page<Client> clientsPage = this.clientRepository.findAll(spec, pageable);

        return clientsPage.map(this.clientMapper::toDto);

    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + id));
        return clientMapper.toDto(client);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Client> clients = clientRepository.findAllById(ids);
        return clients.stream()
                .map(clientMapper::toDto)
                .toList();
    }

    @Transactional
    public ClientResponse updateClient(Long id, ClientUpdateRequest request) {
        Client client = this.clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + id));

        boolean isModified = false;

        if (request.name() != null && !request.name().equalsIgnoreCase(client.getName())) {
            client.setName(request.name());
            isModified = true;
        }

        if (request.email() != null && !request.email().equalsIgnoreCase(client.getEmail())) {

            if (clientRepository.existsByEmail(request.email())) {
                throw new ClientAlreadyExistException("Email " + request.email() + " is already taken");
            }
            client.setEmail(request.email());

            isModified = true;
        }

        Client updatedClient = this.clientRepository.save(client);

        if (isModified){
            ClientUpdatedEvent event = new ClientUpdatedEvent(
                    updatedClient.getId(),
                    updatedClient.getName(),
                    updatedClient.getEmail()
            );

            rabbitTemplate.convertAndSend(
                    rabbitMqProperties.exchange(),
                    rabbitMqProperties.routingKeys().updated(),
                    event
            );
        }

        return clientMapper.toDto(updatedClient);

    }

    @Transactional
    public ClientResponse toggleClientStatus(Long id, boolean isActive) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + id));

        if (client.isActive() == isActive){
            throw new IllegalStateException("Client already has status: " + (isActive ? "active" : "inactive"));
        }

        client.setActive(isActive);
        Client updatedClient = clientRepository.save(client);

        ClientStatusChangedEvent event = new ClientStatusChangedEvent(updatedClient.getId(), updatedClient.isActive());
        this.rabbitTemplate.convertAndSend(
                rabbitMqProperties.exchange(),
                rabbitMqProperties.routingKeys().statusChanged(),
                event
        );

        return clientMapper.toDto(updatedClient);
    }

}
