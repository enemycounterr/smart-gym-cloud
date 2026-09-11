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

    @Value("${application.client-service.url}")
    private String clientServiceUrl;

    @Bean
    public ClientApiClient clientApiClient(ObservationRegistry observationRegistry) {
        RestClient restClient = RestClient.builder()
                .baseUrl(clientServiceUrl)
                .observationRegistry(observationRegistry)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        return HttpServiceProxyFactory.builderFor(adapter)
                .build()
                .createClient(ClientApiClient.class);
    }

}
