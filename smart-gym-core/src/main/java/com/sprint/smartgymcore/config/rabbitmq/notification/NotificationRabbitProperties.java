package com.sprint.smartgymcore.config.rabbitmq.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq.notification")
public record NotificationRabbitProperties(
        String exchange,
        RoutingKeys routingKeys
) {
    public record RoutingKeys(
            String accessRegistered
    ) {}
}
