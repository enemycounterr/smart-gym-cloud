package com.sprint.smartgymcore.config.rabbitmq.notification;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationRabbitProperties.class)
public class NotificationRabbitConfig {
    @Bean
    public TopicExchange notificationExchange(NotificationRabbitProperties props) {
        return new TopicExchange(props.exchange());
    }
}
