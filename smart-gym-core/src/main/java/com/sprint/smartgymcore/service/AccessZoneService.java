package com.sprint.smartgymcore.service;


import com.sprint.smartgymcore.dto.zone.AccessZoneCreateRequest;
import com.sprint.smartgymcore.dto.zone.AccessZoneResponse;
import com.sprint.smartgymcore.dto.zone.AccessZoneUpdateRequest;
import com.sprint.smartgymcore.exceptions.ResourceNotFoundException;
import com.sprint.smartgymcore.external.client.ClientApiClient;
import com.sprint.smartgymcore.external.client.ClientResponse;
import com.sprint.smartgymcore.mapper.AccessZoneMapper;
import com.sprint.smartgymcore.model.AccessZone;
import com.sprint.smartgymcore.repository.AccessLogRepository;
import com.sprint.smartgymcore.repository.AccessZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessZoneService {
    private final AccessZoneRepository zoneRepository;
    private final AccessLogRepository accessLogRepository;
    private final AccessZoneMapper zoneMapper;

    private final ClientApiClient clientApiClient;

    @CacheEvict(value = "zones", allEntries = true)
    @Transactional
    public AccessZoneResponse createZone(AccessZoneCreateRequest request) {
        if (zoneRepository.findByZoneName(request.zoneName().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Zone with name " + request.zoneName() + " already exists");
        }

        AccessZone zone = zoneMapper.toEntity(request);
        return zoneMapper.toDto(zoneRepository.save(zone));
    }

    @CacheEvict(value = "zones", allEntries = true)
    @Transactional
    public AccessZoneResponse renameZone(Long zoneId, AccessZoneUpdateRequest request) {
        AccessZone zone = this.zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with Id: " + zoneId));

        String newName = request.zoneName().toUpperCase();

        if (this.zoneRepository.findByZoneName(newName).isPresent()) {
            throw new IllegalArgumentException("Zone with name " + newName + " already exists");
        }

        zone.setZoneName(newName);
        return this.zoneMapper.toDto(zone);
    }

    @Cacheable(value = "zones", key = "'all'")
    @Transactional(readOnly = true)
    public List<AccessZoneResponse> getAllZones() {
        log.info("--------- METHOD INVOKE getAllZones ---------");
        return zoneRepository.findAll().stream()
                .map(zoneMapper::toDto)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = "zones", allEntries = true)
    @Transactional
    public void deleteZone(Long zoneId) {
        AccessZone zone = this.zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with Id: " + zoneId));

        if (this.accessLogRepository.existsByAccessZoneId(zoneId)) {
            throw new IllegalStateException("Cannot delete zone; It contains logs in AccessLog repository");
        }

        zoneRepository.delete(zone);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByZone(Long zoneId) {
        AccessZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with Id: " + zoneId));

        Set<Long> clientIds = zone.getClientIds();

        if (clientIds.isEmpty()) {
            return List.of();
        }

        return this.clientApiClient.getClientsByIds(clientIds);
    }
}
