package com.sprint.client.mapper;


import com.sprint.client.dto.ClientCreateRequest;
import com.sprint.client.dto.ClientResponse;
import com.sprint.client.model.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientResponse toDto(Client client) {
        if (client == null) return null;

        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.isActive()
        );
    }

    public Client toEntity(ClientCreateRequest request) {
        if (request == null) return null;

        return Client.builder()
                .name(request.name())
                .email(request.email())
                .isActive(true)
                .build();
    }
}
