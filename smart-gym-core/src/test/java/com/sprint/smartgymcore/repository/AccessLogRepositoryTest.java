package com.sprint.smartgymcore.repository;


import com.sprint.smartgymcore.BaseIntegrationTest;
import com.sprint.smartgymcore.model.AccessDirection;
import com.sprint.smartgymcore.model.AccessLog;
import com.sprint.smartgymcore.model.AccessZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccessLogRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Autowired
    private AccessZoneRepository accessZoneRepository;

    @Test
    @Transactional
    public void findCurrentClientsInside_shouldReturnOnlyClientsWithLatestInDirection() {
        Long testClientId = 100L;

        AccessZone zone = AccessZone.builder()
                .zoneName("VIP_ZONE")
                .clientIds(new HashSet<>(Set.of(testClientId)))
                .build();
        zone = accessZoneRepository.save(zone);

        AccessLog log1 = AccessLog.builder()
                .direction(AccessDirection.IN)
                .clientId(testClientId)
                .accessZone(zone)
                .timeStamp(Instant.now().minus(30, ChronoUnit.MINUTES))
                .build();

        AccessLog log2 = AccessLog.builder()
                .direction(AccessDirection.OUT)
                .clientId(testClientId)
                .accessZone(zone)
                .timeStamp(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        AccessLog log3 = AccessLog.builder()
                .direction(AccessDirection.IN)
                .clientId(testClientId)
                .accessZone(zone)
                .timeStamp(Instant.now())
                .build();

        accessLogRepository.saveAll(List.of(log1, log2, log3));

        List<AccessLog> insideClients = accessLogRepository.findCurrentClientsInside(AccessDirection.IN);

        assertEquals(1, insideClients.size());
        assertEquals("VIP_ZONE", insideClients.get(0).getAccessZone().getZoneName());
        assertEquals(testClientId, insideClients.get(0).getClientId());
    }

    @Test
    @Transactional
    public void countByClientIdAndDirection_shouldCountCorrectlyByEnum() {

        Long testClientId = 200L;

        AccessZone zone = AccessZone.builder()
                .zoneName("COUNT_ZONE")
                .clientIds(new HashSet<>(Set.of(testClientId)))
                .build();
        zone = accessZoneRepository.save(zone);

        AccessLog log1 = AccessLog.builder()
                .direction(AccessDirection.IN)
                .clientId(testClientId)
                .accessZone(zone)
                .timeStamp(Instant.now())
                .build();

        AccessLog log2 = AccessLog.builder()
                .direction(AccessDirection.OUT)
                .clientId(testClientId)
                .accessZone(zone)
                .timeStamp(Instant.now())
                .build();

        AccessLog log3 = AccessLog.builder()
                .direction(AccessDirection.IN)
                .clientId(testClientId)
                .accessZone(zone)
                .timeStamp(Instant.now())
                .build();

        accessLogRepository.saveAll(List.of(log1, log2, log3));

        long inCount = accessLogRepository.countByClientIdAndDirection(testClientId, AccessDirection.IN);
        long outCount = accessLogRepository.countByClientIdAndDirection(testClientId, AccessDirection.OUT);

        assertEquals(2, inCount);
        assertEquals(1, outCount);
    }
}
