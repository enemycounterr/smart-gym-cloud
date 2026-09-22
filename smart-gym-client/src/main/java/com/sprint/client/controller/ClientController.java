package com.sprint.client.controller;


import com.sprint.client.dto.ClientCreateRequest;
import com.sprint.client.dto.ClientResponse;
import com.sprint.client.dto.ClientUpdateRequest;
import com.sprint.client.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GUARD')")
    public ResponseEntity<Page<ClientResponse>> getAllClients(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(this.clientService.getAllClients(search, pageable));
    }

    @GetMapping("/{id}")
    public ClientResponse getClientById(@PathVariable Long id) {
        return this.clientService.getClientById(id);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ClientResponse>> getClientsByIds(@RequestBody Set<Long> ids){
        return ResponseEntity.ok(this.clientService.getClientsByIds(ids));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ClientResponse create(@Valid @RequestBody ClientCreateRequest request) {
        return this.clientService.createClient(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientUpdateRequest request) {
        return this.clientService.updateClient(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientResponse changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        return this.clientService.toggleClientStatus(id, active);
    }




}
