//package com.sprint.training.messaging.listener;
//
//import com.sprint.training.BaseIntegrationTest;
//import com.sprint.training.dto.access.AccessCheckRequest;
//import com.sprint.training.model.AccessCard;
//import com.sprint.training.model.AccessDirection;
//import com.sprint.training.model.AccessZone;
//import com.sprint.training.model.Client;
//import com.sprint.training.repository.AccessCardRepository;
//import com.sprint.training.repository.AccessZoneRepository;
//import com.sprint.training.repository.ClientRepository;
//import com.sprint.training.service.AccessService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.time.Duration;
//
//import static org.awaitility.Awaitility.await;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//
////public class AccessNotificationListenerIntegrationTest extends BaseIntegrationTest {
////
////    @Autowired
////    private AccessService accessService;
////
////    @Autowired
////    private ClientRepository clientRepository;
////
////    @Autowired
////    private AccessCardRepository accessCardRepository;
////
////    @Autowired
////    private AccessZoneRepository accessZoneRepository;
////
////    private String testRfidToken = "RFID_TEST_999";
////    private Long testZoneId;
////
////    @BeforeEach
////    void setUp() {
////        AccessZone zone = new AccessZone();
////        zone.setZoneName("Cardio Zone");
////
////        zone = accessZoneRepository.save(zone);
////
////        testZoneId = zone.getId();
////
////        Client client = new Client(null, "Integration Test User", "testIntegration@gmail.com", true);
////        client.addAccessZone(zone);
////        clientRepository.save(client);
////
////        AccessCard card = new AccessCard(testRfidToken, client);
////        accessCardRepository.save(card);
////    }
////
////    @Test
////    void shouldSendMessageToRabbitMqAndTriggerCrmClient_whenAccessIsRegistered() {
////
////        AccessCheckRequest request = new AccessCheckRequest(testRfidToken, testZoneId, AccessDirection.IN);
////
////        accessService.registerAccess(request);
////
////        await().atMost(Duration.ofSeconds(5))
////                .untilAsserted(() -> verify(crmClient).sendLoyaltyPoints(anyLong(), anyString()));
////    }
////
////    @Test
////    void shouldNotSendLoyaltyPointsToCrm_whenAccessDirectionIsOut() {
////
////        AccessCheckRequest request = new AccessCheckRequest(testRfidToken, testZoneId, AccessDirection.OUT);
////
////        accessService.registerAccess(request);
////
////        await().atMost(Duration.ofSeconds(5))
////                .untilAsserted(() -> verify(crmClient, never()).sendLoyaltyPoints(anyLong(), anyString()));
////    }
////}
