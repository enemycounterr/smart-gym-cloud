package com.sprint.smartgymcore.service;


import com.sprint.smartgymcore.config.rabbitmq.notification.NotificationRabbitProperties;
import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.dto.access.AccessLogResponse;
import com.sprint.smartgymcore.dto.access.ClientAccessStatsResponse;
import com.sprint.smartgymcore.dto.access.ClientInsideResponse;
import com.sprint.smartgymcore.exceptions.AccessAlreadyGrantedException;
import com.sprint.smartgymcore.exceptions.ResourceNotFoundException;
import com.sprint.smartgymcore.exceptions.ZoneAccessDeniedException;
import com.sprint.smartgymcore.external.client.ClientApiClient;
import com.sprint.smartgymcore.external.client.ClientResponse;
import com.sprint.smartgymcore.mapper.AccessMapper;
import com.sprint.smartgymcore.messaging.event.inbound.client.ClientCreatedEvent;
import com.sprint.smartgymcore.messaging.event.inbound.client.ClientStatusChangedEvent;
import com.sprint.smartgymcore.messaging.event.outbound.AccessRegisterEvent;
import com.sprint.smartgymcore.metrics.service.MetricsService;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.model.AccessDirection;
import com.sprint.smartgymcore.model.AccessLog;
import com.sprint.smartgymcore.model.AccessZone;
import com.sprint.smartgymcore.repository.AccessCardRepository;
import com.sprint.smartgymcore.repository.AccessLogRepository;
import com.sprint.smartgymcore.repository.AccessZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessService {
    private final AccessLogRepository accessLogRepository;
    private final AccessZoneRepository accessZoneRepository;
    private final AccessCardRepository accessCardRepository;

    private final AccessMapper accessMapper;

    private final RabbitTemplate rabbitTemplate;

    private final MetricsService metricsService;

    private final NotificationRabbitProperties notificationRabbitProperties;
    private final ClientApiClient clientApiClient;

    @Transactional
    public void processClientCreated(ClientCreatedEvent event) {
        log.info("Received event to create Access Card for clientId: {}", event.clientId());

        String generatedToken = "RFID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AccessCard card = AccessCard.builder()
                .rfidToken(generatedToken)
                .clientId(event.clientId())
                .clientName(event.name())
                .isActive(true)
                .issuedAt(LocalDateTime.now())
                .build();

        accessCardRepository.save(card);
        log.info("Successfully created Access Card [{}] for clientId: {}", generatedToken, event.clientId());
    }

    @Transactional
    public void processClientStatusChanged(ClientStatusChangedEvent event) {
        log.info(
                "Received event to update Access Card status for clientId: {}. New status: {}",
                event.clientId(), event.isActive()
        );

        accessCardRepository.findByClientId(event.clientId())
                .ifPresentOrElse(card -> {
                    card.setActive(event.isActive());
                    accessCardRepository.save(card);
                    log.info("Updated Access Card status to {} for clientId: {}", event.isActive(), event.clientId());
                }, () -> log.warn("Cannot update status: Access card not found for clientId: {}", event.clientId()));
    }

    @Transactional(readOnly = true)
    public Page<AccessLogResponse> getAllLogs(Pageable pageable) {
        Page<AccessLog> logsPage = this.accessLogRepository.findAll(pageable);

        if (logsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<Long> clientIds = logsPage.stream()
                .map(AccessLog::getClientId)
                .collect(Collectors.toSet());

        Map<Long, ClientResponse> clientMap = getClientsBatch(clientIds);

        return logsPage.map(log -> {
            ClientResponse client = clientMap.get(log.getClientId());
            String clientName = (client != null) ? client.name() : "Unknown Client";
            return accessMapper.toDto(log, clientName);
        });
    }

    @Transactional
    public void grantAccessToZone(Long clientId, Long zoneId) {

        fetchClientSafely(clientId);

        if (this.accessZoneRepository.hasClientAccess(zoneId, clientId)) {
            AccessZone zone = accessZoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + zoneId));
            throw new AccessAlreadyGrantedException(
                    "Client with ID " + clientId + " already has access to zone " + zone.getZoneName()
            );
        }

        AccessZone zone = accessZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + zoneId));

        zone.getClientIds().add(clientId);
        this.accessZoneRepository.save(zone);
    }

    @Transactional
    public void revokeAccessFromZone(Long clientId, Long zoneId) {
        AccessZone zone = accessZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + zoneId));

        zone.getClientIds().remove(clientId);
        this.accessZoneRepository.save(zone);
    }

    @Transactional
    @CacheEvict(value = "clientStats", key = "#result.clientId")
    public AccessLogResponse registerAccess(AccessCheckRequest request) {
        log.info("Processing access registration for RFID: {}", request.rfidToken());

        AccessCard card = accessCardRepository.findByRfidToken(request.rfidToken())
                .orElseThrow(() -> new ResourceNotFoundException(
                                "Access card not found with token:  " + request.rfidToken()
                        )
                );

        if (!card.isActive()) {
            throw new ZoneAccessDeniedException("Access denied! Card is inactive!");
        }

        Long currentClientId = card.getClientId();
        String currentClientName = card.getClientName();

        AccessZone accessZone = accessZoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with ID: " + request.zoneId()));

        if (!this.accessZoneRepository.hasClientAccess(request.zoneId(), currentClientId)) {
            throw new ZoneAccessDeniedException("Access denied! Client '" + currentClientName +
                    "' does not have permission for zone '" + accessZone.getZoneName() + "'");
        }

        Optional<AccessLog> latestLog = this.accessLogRepository.findFirstByClientIdOrderByTimeStampDesc(currentClientId);

        if (latestLog.isPresent() && latestLog.get().getDirection() == request.direction()) {
            throw new ZoneAccessDeniedException("Anti-Passback violation! Client '" + currentClientName +
                    "' already performed direction: " + request.direction());
        }

        AccessLog accessLog = this.accessMapper.toEntity(request, currentClientId, accessZone);
        AccessLog savedLog = this.accessLogRepository.save(accessLog);

        this.metricsService.incrementAccessEventReceived(request.direction());

        AccessRegisterEvent event = new AccessRegisterEvent(
                savedLog.getId(),
                currentClientId,
                currentClientName,
                accessZone.getZoneName(),
                savedLog.getDirection().name(),
                savedLog.getTimeStamp()
        );

        log.info("Access successfully registered, sending event to RabbitMQ");
        this.rabbitTemplate.convertAndSend(
                notificationRabbitProperties.exchange(),
                notificationRabbitProperties.routingKeys().accessRegistered(),
                event
        );

        return accessMapper.toDto(savedLog, currentClientName);
    }

    @Cacheable(value = "clientStats", key = "#clientId")
    @Transactional(readOnly = true)
    public ClientAccessStatsResponse getClientStats(Long clientId) {
        log.info("-----------------METHOD INVOKE getClientStats ------------------------");

        ClientResponse client = fetchClientSafely(clientId);

        long inCount = this.accessLogRepository.countByClientIdAndDirection(clientId, AccessDirection.IN);

        long outCount = this.accessLogRepository.countByClientIdAndDirection(clientId, AccessDirection.OUT);

        return new ClientAccessStatsResponse(clientId, client.name(), inCount, outCount);
    }

    @Transactional(readOnly = true)
    public List<ClientInsideResponse> getClientInside() {
        List<AccessLog> insideLogs = this.accessLogRepository.findCurrentClientsInside(AccessDirection.IN);

        if (insideLogs.isEmpty()) {
            return List.of();
        }

        Set<Long> clientIds = insideLogs.stream()
                .map(AccessLog::getClientId)
                .collect(Collectors.toSet());

        Map<Long, ClientResponse> clientsMap = getClientsBatch(clientIds);

        return insideLogs.stream()
                .map(log -> {
                            ClientResponse client = clientsMap.get(log.getClientId());
                            String name = (client != null) ? client.name() : "Unknown Client";
                            return new ClientInsideResponse(
                                    log.getClientId(),
                                    name,
                                    log.getTimeStamp()
                            );
                        }
                )
                .sorted(Comparator.comparing(ClientInsideResponse::insideSince))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientByLogId(Long logId) {
        AccessLog log = this.accessLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Access log not found with Id: " + logId));

        return fetchClientSafely(log.getClientId());
    }

    @Transactional(readOnly = true)
    public String getRfidTokenByClientId(Long clientId) {
        AccessCard card = accessCardRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Access card not found for client ID: " + clientId
                ));

        return card.getRfidToken();
    }

    private Map<Long, ClientResponse> getClientsBatch(Set<Long> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<ClientResponse> clients = clientApiClient.getClientsByIds(clientIds);

            return clients.stream()
                    .collect(Collectors.toMap(ClientResponse::id, Function.identity()));
        } catch (Exception e) {
            log.error("Failed to fetch clients batch: {}", e.getMessage());
            return Map.of();
        }
    }

    private ClientResponse fetchClientSafely(Long clientId) {
        try {
            return clientApiClient.getClientById(clientId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Client not found with ID: " + clientId);
            }
            throw e;
        }
    }


}
