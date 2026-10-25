package com.sprint.smartgymcore.service;


import com.sprint.smartgymcore.BaseIntegrationTest;
import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.dto.access.ClientAccessStatsResponse;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.model.AccessDirection;
import com.sprint.smartgymcore.model.AccessZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//@WireMockTest(httpPort = 8082)
public class AccessServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AccessService accessService;

    private final Long testClientId = 666L;
    private AccessZone savedZone;
    private String rfidToken;

    @BeforeEach
    void setUp() {
        AccessZone zone = AccessZone.builder()
                .zoneName("GYM_INTEGRATION_ZONE")
                .clientIds(new HashSet<>(Set.of(testClientId)))
                .build();
        savedZone = accessZoneRepository.save(zone);

        rfidToken = "RFID-INT-999";
        AccessCard card = AccessCard.builder()
                .rfidToken(rfidToken)
                .clientId(testClientId)
                .clientName("Integration User")
                .isActive(true)
                .issuedAt(LocalDateTime.now())
                .build();
        accessCardRepository.save(card);

    }

    @Test
    @DisplayName("Must cache statistics in Redis and invalidate the cache when passing through the turnstile")
    void getClientStats_shouldCacheInRedisAndEvictOnNewAccess() {

        ClientAccessStatsResponse stats1 = accessService.getClientStats(testClientId);
        assertNotNull(stats1);
        assertEquals(0, stats1.totalEntries());

        String redisKey = "clientStats::" + testClientId;
        Boolean hasKeyInRedis = redisTemplate.hasKey(redisKey);
        assertEquals(Boolean.TRUE, hasKeyInRedis, "The key must be present in Redis after calling the method");

        AccessCheckRequest request = new AccessCheckRequest(rfidToken, savedZone.getId(), AccessDirection.IN);
        accessService.registerAccess(request);

        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertEquals(Boolean.FALSE, redisTemplate.hasKey(redisKey), "The key exists after evict must be false"));

        ClientAccessStatsResponse stats2 = accessService.getClientStats(testClientId);
        assertEquals(1, stats2.totalEntries(), "Statistics should update to 1 entry");

    }
}
