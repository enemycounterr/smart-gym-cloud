package com.sprint.smartgymcore;

import com.sprint.smartgymcore.configuration.IntegrationContainersConfiguration;
import com.sprint.smartgymcore.repository.AccessCardRepository;
import com.sprint.smartgymcore.repository.AccessLogRepository;
import com.sprint.smartgymcore.repository.AccessZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(IntegrationContainersConfiguration.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected AccessLogRepository accessLogRepository;

    @Autowired
    protected AccessCardRepository accessCardRepository;

    @Autowired
    protected AccessZoneRepository accessZoneRepository;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanDatabase() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });

        accessLogRepository.deleteAll();
        accessCardRepository.deleteAll();
        accessZoneRepository.deleteAll();
    }

}
