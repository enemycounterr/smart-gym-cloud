package com.sprint.smartgymcore.external.client;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ExternalClientConfig {

    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Value("${application.client-service.url}")
    private String clientServiceUrl;

    @Value("${application.client-service.api-key}")
    private String internalApiKey;

    @Bean
    public ClientApiClient clientApiClient(ObservationRegistry observationRegistry) {
        RestClient restClient = RestClient.builder()
                .baseUrl(clientServiceUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKey)
                .observationRegistry(observationRegistry)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        return HttpServiceProxyFactory.builderFor(adapter)
                .build()
                .createClient(ClientApiClient.class);
    }

}
