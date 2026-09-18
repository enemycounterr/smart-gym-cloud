package com.sprint.smartgymcore.config;

import com.sprint.smartgymcore.external.client.ClientApiClient;
import com.sprint.smartgymcore.external.client.ClientResponse;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.repository.AccessCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientNameSyncRunner implements ApplicationRunner {
    private final AccessCardRepository accessCardRepository;
    private final ClientApiClient clientApiClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<AccessCard> unSyncedCards = accessCardRepository.findAllByClientName("Unknown Client");

        if (unSyncedCards.isEmpty()) {
            log.info("All access cards are already synchronized with client names.");
            return;
        }

        log.info("Found {} cards without names. Starting background synchronization with Client Service", unSyncedCards.size());

        try {
            Set<Long> clientIds = unSyncedCards.stream()
                    .map(AccessCard::getClientId)
                    .collect(Collectors.toSet());

            List<ClientResponse> clients = clientApiClient.getClientsByIds(clientIds);

            Map<Long, ClientResponse> clientMap = clients.stream()
                    .collect(Collectors.toMap(ClientResponse::id, Function.identity()));

            int updatedCount = 0;
            for (AccessCard card : unSyncedCards) {
                ClientResponse client = clientMap.get(card.getClientId());
                if (client != null && client.name() != null) {
                    card.setClientName(client.name());
                    updatedCount++;
                }
            }

            accessCardRepository.saveAll(unSyncedCards);
            log.info("Successfully synchronized names for {} access cards!", updatedCount);

        } catch (Exception e) {
            log.warn("Failed to synchronize client names at startup: {}. " +
                    "Cards will continue to work with default name.", e.getMessage());
        }
    }
}
