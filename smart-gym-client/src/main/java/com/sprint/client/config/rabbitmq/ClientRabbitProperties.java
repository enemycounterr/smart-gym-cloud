package com.sprint.client.config.rabbitmq;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record ClientRabbitProperties(
        String exchange,
        RoutingKeys routingKeys
) {
    public record RoutingKeys(
            String created,
            String statusChanged,
            String updated
    ){}
}
