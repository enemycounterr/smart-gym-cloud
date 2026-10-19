package com.sprint.smartgymcore.config.rabbitmq.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rabbitmq.notification")
public record NotificationRabbitProperties(

        @NotBlank(message = "Exchange name must not be blank")
        String exchange,

        @Valid
        @NotNull(message = "RoutingKeys section is completely missing")
        RoutingKeys routingKeys
) {
    public record RoutingKeys(
            @NotBlank(message = "Routing key for access-registered must not be blank")
            String accessRegistered
    ) {}
}
