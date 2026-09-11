package com.sprint.smartgymcore.controller;


import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.dto.access.AccessLogResponse;
import com.sprint.smartgymcore.dto.access.ClientAccessStatsResponse;
import com.sprint.smartgymcore.dto.access.ClientInsideResponse;
import com.sprint.smartgymcore.external.client.ClientResponse;
import com.sprint.smartgymcore.service.AccessService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {
    private final AccessService accessService;

    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARD')")
    public ResponseEntity<Page<AccessLogResponse>> getAllLogs(
            @PageableDefault(size = 20, sort = "timeStamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(this.accessService.getAllLogs(pageable));
    }

    @PostMapping("/clients/{clientId}/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void grantAccess(@PathVariable Long clientId, @PathVariable Long zoneId) {
        this.accessService.grantAccessToZone(clientId, zoneId);
    }

    @DeleteMapping("/clients/{clientId}/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void revokeAccess(@PathVariable Long clientId, @PathVariable Long zoneId) {
        this.accessService.revokeAccessFromZone(clientId, zoneId);
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARD')")
    public AccessLogResponse register(@Valid @RequestBody AccessCheckRequest request) {
        return this.accessService.registerAccess(request);
    }

    @GetMapping("/clients/{clientId}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARD')")
    public ClientAccessStatsResponse getStats(@PathVariable Long clientId) {
        return this.accessService.getClientStats(clientId);
    }

    @GetMapping("/inside")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARD')")
    public List<ClientInsideResponse> getClientInside() {
        return this.accessService.getClientInside();
    }

    @GetMapping("logs/{logId}/client")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARD')")
    public ClientResponse getClientByLogId(@PathVariable Long logId) {
        return this.accessService.getClientByLogId(logId);
    }

    @GetMapping("/clients/{clientId}/rfid")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARD')")
    public ResponseEntity<Map<String, String>> getRfidTokenByClientId(@PathVariable Long clientId) {
        String token = this.accessService.getRfidTokenByClientId(clientId);

        return ResponseEntity.ok(Map.of("rfidToken", token));
    }
}
