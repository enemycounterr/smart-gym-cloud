package com.sprint.client.controller;

import com.sprint.client.dto.ClientResponse;
import com.sprint.client.service.ClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/v1/clients")
@Validated
@RequiredArgsConstructor
public class InternalClientController {

    private final ClientService clientService;

    @GetMapping("/{id}")
    public ClientResponse getClientById(@PathVariable Long id) {
        return this.clientService.getClientById(id);
    }

    @PostMapping("/batch")
    public List<ClientResponse> getClientsByIds(@Valid @RequestBody @NotEmpty @Size(max = 500) Set<Long> ids) {
        return this.clientService.getClientsByIds(ids);
    }
}
