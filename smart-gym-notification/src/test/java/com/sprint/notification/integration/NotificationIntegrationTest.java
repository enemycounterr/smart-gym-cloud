package com.sprint.notification.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.sprint.notification.config.IntegrationContainersConfiguration;
import com.sprint.notification.dto.AccessRegisterEvent;
import com.sprint.notification.exception.CrmIntegrationException;
import com.sprint.notification.service.RabbitAdminService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(IntegrationContainersConfiguration.class)
public class NotificationIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("integration.crm.base-url", wireMock::baseUrl);
    }

    @Autowired
    private CrmClient crmClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitAdminService rabbitAdminService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.queue}")
    private String mainQueue;

    @Value("${app.rabbitmq.dlq}")
    private String dlqQueue;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        circuitBreakerRegistry.circuitBreaker("crmService").reset();
        amqpAdmin.purgeQueue(mainQueue, false);
        amqpAdmin.purgeQueue(dlqQueue, false);
    }

    @Test
    @DisplayName("RabbitMQ -> Listener -> CrmClient: should send loyalty points when direction is IN")
    void handleAccessEvent_whenDirectionIsIn_shouldCallCrm() {

        wireMock.stubFor(post(urlEqualTo("/post"))
                .willReturn(aResponse().withStatus(200)));

        AccessRegisterEvent event = new AccessRegisterEvent(
                1L, 42L, "Filthy", "Main Hall", "IN", Instant.now()
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                wireMock.verify(1,
                        postRequestedFor(urlEqualTo("/post"))
                        .withRequestBody(matchingJsonPath("$.clientId", equalTo("42")))
                        .withRequestBody(matchingJsonPath("$.name", equalTo("Filthy")))
                        .withRequestBody(matchingJsonPath("$.points", equalTo("10")))
                )
        );

        assertEquals(0, getQueueMessageCount(dlqQueue));
    }

    @Test
    @DisplayName("RabbitMQ -> Listener: should ignore event and not call CRM when direction is OUT")
    void handleAccessEvent_whenDirectionIsOut_shouldIgnoreEvent() {

        AccessRegisterEvent event = new AccessRegisterEvent(
                2L, 42L, "Filthy", "Main Hall", "OUT", Instant.now()
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertEquals(0, getQueueMessageCount(mainQueue))
        );

        wireMock.verify(0, postRequestedFor(urlEqualTo("/post")));
        assertEquals(0, getQueueMessageCount(dlqQueue));

    }

    @Test
    @DisplayName("CrmClient @Retry: should retry 3 times on HTTP 500 and throw CrmIntegrationException")
    void sendLoyaltyPoints_whenCrmReturns500_shouldRetryThreeTimesAndFail() {

        wireMock.stubFor(post(urlEqualTo("/post"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(CrmIntegrationException.class,
                () -> crmClient.sendLoyaltyPoints(10L, "Filthy"));

        wireMock.verify(3, postRequestedFor(urlEqualTo("/post")));

    }

    @Test
    @DisplayName("CrmClient: should not retry or trip CircuitBreaker on HTTP 400")
    void sendLoyaltyPoints_whenCrmReturns400_shouldNotRetryAndStayClosed() {
        wireMock.stubFor(post(urlEqualTo("/post"))
                .willReturn(aResponse().withStatus(400)));

        assertThrows(CrmIntegrationException.class,
                () -> crmClient.sendLoyaltyPoints(10L, "Filthy"));

        wireMock.verify(1, postRequestedFor(urlEqualTo("/post")));
        assertEquals(CircuitBreaker.State.CLOSED,
                circuitBreakerRegistry.circuitBreaker("crmService").getState());
    }

    @Test
    @DisplayName("CrmClient @CircuitBreaker: should transition to OPEN after failures and fast-fail")
    void sendLoyaltyPoints_whenFailuresExceedThreshold_shouldOpenCircuitBreaker() {

        wireMock.stubFor(post(urlEqualTo("/post"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(CrmIntegrationException.class, () -> crmClient.sendLoyaltyPoints(10L, "Filthy"));
        assertThrows(CrmIntegrationException.class, () -> crmClient.sendLoyaltyPoints(10L, "Filthy"));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("crmService");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        wireMock.resetRequests();

        CrmIntegrationException ex = assertThrows(CrmIntegrationException.class,
                () -> crmClient.sendLoyaltyPoints(10L, "Filthy"));

        assertInstanceOf(CallNotPermittedException.class, ex.getCause());
        wireMock.verify(0, postRequestedFor(urlEqualTo("/post")));

    }

    @Test
    @DisplayName("DLQ End-to-End: should route failed event to DLQ and reprocess after CRM recovery")
    void handleAccessEvent_whenCrmFails_shouldRouteToDlqAndReprocessOnRecovery() {
        wireMock.stubFor(post(urlEqualTo("/post"))
                .willReturn(aResponse().withStatus(500)));

        AccessRegisterEvent event = new AccessRegisterEvent(
                99L, 77L, "Filthy", "Cardio", "IN", Instant.now()
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMock.verify(3, postRequestedFor(urlEqualTo("/post")));
            assertEquals(1, getQueueMessageCount(dlqQueue));
        });

        wireMock.resetAll();
        wireMock.stubFor(post(urlEqualTo("/post"))
                .willReturn(aResponse().withStatus(200)));

        int reprocessed = rabbitAdminService.reprocessDlqMessages();
        assertEquals(1, reprocessed);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            wireMock.verify(1, postRequestedFor(urlEqualTo("/post"))
                    .withRequestBody(matchingJsonPath("$.clientId", equalTo("77"))));
            assertEquals(0, getQueueMessageCount(mainQueue));
            assertEquals(0, getQueueMessageCount(dlqQueue));
        });
    }

    private long getQueueMessageCount(String queueName) {
        QueueInformation info = amqpAdmin.getQueueInfo(queueName);
        return info != null ? info.getMessageCount() : 0;
    }

}
