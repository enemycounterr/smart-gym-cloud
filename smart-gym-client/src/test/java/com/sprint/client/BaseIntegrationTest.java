package com.sprint.client;


import com.sprint.client.config.IntegrationContainersConfiguration;
import com.sprint.client.outbox.repository.OutboxRepository;
import com.sprint.client.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(IntegrationContainersConfiguration.class)
public class BaseIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected OutboxRepository outboxRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE clients, outbox_events RESTART IDENTITY CASCADE");
    }


}
