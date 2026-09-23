package com.sprint.client.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        String exchange,
        RoutingKeys routingKeys
) {
    public record RoutingKeys(
            String created,
            String statusChanged,
            String updated
    ){}
}
