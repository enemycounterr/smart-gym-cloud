package com.sprint.smartgymcore.external.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Set;

@HttpExchange("/api/v1/clients")
public interface ClientApiClient {

    @GetExchange("/{id}")
    ClientResponse getClientById(@PathVariable("id") Long id);

    @PostExchange("/batch")
    List<ClientResponse> getClientsByIds(@RequestBody Set<Long> ids);
}
