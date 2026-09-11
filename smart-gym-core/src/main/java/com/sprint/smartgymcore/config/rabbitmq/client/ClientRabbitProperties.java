package com.sprint.smartgymcore.config.rabbitmq.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq.client")
public record ClientRabbitProperties(
        String exchange,
        Queues queues,
        RoutingKeys routingKeys
) {
    public record Queues(String clientCreated, String clientStatus) {}
    public record RoutingKeys(String created, String statusChanged) {}
}
