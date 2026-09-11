package com.sprint.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMqConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;
    @Value("${app.rabbitmq.queue}")
    private String queueName;
    @Value("${app.rabbitmq.binding-pattern}")
    private String bindingPattern;
    @Value("${app.rabbitmq.dlx}")
    private String dlxName;
    @Value("${app.rabbitmq.dlq}")
    private String dlqName;

    //================
    //Main QUEUE
    //================
    @Bean
    public TopicExchange gymExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", queueName)
                .build();
    }

    @Bean
    public Binding bindingNotifications(Queue notificationsQueue, TopicExchange gymExchange) {
        return BindingBuilder
                .bind(notificationsQueue)
                .to(gymExchange)
                .with(bindingPattern);
    }

    //================
    //DLQ
    //================
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxName);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(queueName);
    }

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }


}
