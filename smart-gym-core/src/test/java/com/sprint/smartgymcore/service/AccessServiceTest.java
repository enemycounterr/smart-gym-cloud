package com.sprint.smartgymcore.service;


import com.sprint.smartgymcore.config.rabbitmq.notification.NotificationRabbitProperties;
import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.dto.access.AccessLogResponse;
import com.sprint.smartgymcore.exceptions.ZoneAccessDeniedException;
import com.sprint.smartgymcore.external.client.ClientApiClient;
import com.sprint.smartgymcore.external.client.ClientResponse;
import com.sprint.smartgymcore.mapper.AccessMapper;
import com.sprint.smartgymcore.metrics.service.MetricsService;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.model.AccessDirection;
import com.sprint.smartgymcore.model.AccessLog;
import com.sprint.smartgymcore.model.AccessZone;
import com.sprint.smartgymcore.repository.AccessCardRepository;
import com.sprint.smartgymcore.repository.AccessLogRepository;
import com.sprint.smartgymcore.repository.AccessZoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessServiceTest {

    @Mock
    private AccessCardRepository accessCardRepository;
    @Mock
    private AccessLogRepository accessLogRepository;
    @Mock
    private AccessZoneRepository accessZoneRepository;
    @Mock
    private AccessMapper accessMapper;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private MetricsService metricsService;
    @Mock
    private NotificationRabbitProperties notificationRabbitProperties;
    @Mock
    private ClientApiClient clientApiClient;

    @InjectMocks
    private AccessService accessService;

    private ClientResponse createMockClientResponse() {
        return new ClientResponse(1L, "Danek", "danek@mail.com", true);
    }

    private AccessCard createMockCard(String token) {
        return AccessCard.builder()
                .rfidToken(token)
                .clientId(1L)
                .isActive(true)
                .build();
    }

    private AccessZone createMockZone() {
        return AccessZone.builder()
                .id(1L)
                .zoneName("GYM")
                .clientIds(new HashSet<>(Set.of(1L)))
                .build();
    }

    private void mockNotificationRabbitProperties() {
        NotificationRabbitProperties.RoutingKeys routingKeys = mock(NotificationRabbitProperties.RoutingKeys.class);
        when(notificationRabbitProperties.exchange()).thenReturn("gym.events");
        when(notificationRabbitProperties.routingKeys()).thenReturn(routingKeys);
        when(routingKeys.accessRegistered()).thenReturn("gym.access.registered");
    }

    @Test
    @DisplayName("registerAccess: should throw ZoneAccessDeniedException on sequential IN swipes (anti-passback)")
    public void registerAccess_whenDoubleIn_shouldThrowZoneAccessDeniedException() {
        String token = "RFID-123";
        AccessCard card = createMockCard(token);
        AccessZone zone = createMockZone();

        AccessCheckRequest request = new AccessCheckRequest(token, 1L, AccessDirection.IN);

        AccessLog lastLog = AccessLog.builder()
                .direction(AccessDirection.IN)
                .clientId(1L)
                .accessZone(zone)
                .build();

        when(accessCardRepository.findByRfidToken(token)).thenReturn(Optional.of(card));
        when(clientApiClient.getClientById(1L)).thenReturn(createMockClientResponse());

        when(accessZoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(accessZoneRepository.hasClientAccess(1L, 1L)).thenReturn(true);
        when(accessLogRepository.findFirstByClientIdOrderByTimeStampDesc(1L)).thenReturn(Optional.of(lastLog));

        assertThrows(ZoneAccessDeniedException.class, () -> accessService.registerAccess(request));
        verifyNoInteractions(rabbitTemplate, metricsService);
    }

    @Test
    @DisplayName("registerAccess: should throw ZoneAccessDeniedException on sequential OUT swipes (anti-passback)")
    public void registerAccess_whenDoubleOut_shouldThrowZoneAccessDeniedException() {
        String token = "RFID-123";
        AccessCard card = createMockCard(token);
        AccessZone zone = createMockZone();

        AccessCheckRequest request = new AccessCheckRequest(token, 1L, AccessDirection.OUT);
        AccessLog lastLog = AccessLog.builder()
                .direction(AccessDirection.OUT)
                .clientId(1L)
                .accessZone(zone)
                .build();

        when(accessCardRepository.findByRfidToken(token)).thenReturn(Optional.of(card));
        when(clientApiClient.getClientById(1L)).thenReturn(createMockClientResponse());
        when(accessZoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(accessZoneRepository.hasClientAccess(1L, 1L)).thenReturn(true);
        when(accessLogRepository.findFirstByClientIdOrderByTimeStampDesc(1L)).thenReturn(Optional.of(lastLog));

        assertThrows(ZoneAccessDeniedException.class, () -> accessService.registerAccess(request));
        verifyNoInteractions(rabbitTemplate, metricsService);
    }

    @Test
    @DisplayName("registerAccess: should register access and trigger messaging and metrics when valid")
    public void registerAccess_whenInAfterOut_shouldSucceed() {
        String token = "RFID-123";
        AccessCard card = createMockCard(token);
        AccessZone zone = createMockZone();

        AccessCheckRequest request = new AccessCheckRequest(token, 1L, AccessDirection.IN);

        AccessLog lastLog = AccessLog.builder()
                .direction(AccessDirection.OUT)
                .clientId(1L)
                .accessZone(zone)
                .build();

        AccessLog savedLog = AccessLog.builder()
                .id(100L)
                .direction(AccessDirection.IN)
                .clientId(1L)
                .accessZone(zone)
                .timeStamp(Instant.now())
                .build();

        AccessLogResponse expectedResponse = new AccessLogResponse(
                100L,
                1L,
                "Danek",
                "GYM",
                AccessDirection.IN,
                savedLog.getTimeStamp()
        );

        when(accessCardRepository.findByRfidToken(token)).thenReturn(Optional.of(card));
        when(clientApiClient.getClientById(1L)).thenReturn(createMockClientResponse());
        when(accessZoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(accessZoneRepository.hasClientAccess(1L, 1L)).thenReturn(true);
        when(accessLogRepository.findFirstByClientIdOrderByTimeStampDesc(1L)).thenReturn(Optional.of(lastLog));

        when(accessMapper.toEntity(request, 1L, zone)).thenReturn(savedLog);
        when(accessLogRepository.save(savedLog)).thenReturn(savedLog);
        when(accessMapper.toDto(savedLog, "Danek")).thenReturn(expectedResponse);
        mockNotificationRabbitProperties();

        AccessLogResponse result = accessService.registerAccess(request);

        assertNotNull(result);
        assertEquals(AccessDirection.IN, result.direction());
        assertEquals(expectedResponse, result);

        verify(accessLogRepository).save(savedLog);
        verify(metricsService).incrementAccessEventReceived(AccessDirection.IN);
        verify(rabbitTemplate).convertAndSend(eq("gym.events"), eq("gym.access.registered"), any(Object.class));
    }

}
