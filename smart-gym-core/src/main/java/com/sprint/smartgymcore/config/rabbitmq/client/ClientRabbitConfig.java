package com.sprint.smartgymcore.config.rabbitmq.client;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClientRabbitProperties.class)
public class ClientRabbitConfig {

    @Bean
    public DirectExchange clientDirectExchange(ClientRabbitProperties props) {
        return new DirectExchange(props.exchange());
    }

    @Bean
    public Queue clientCreatedQueue(ClientRabbitProperties props) {
        return new Queue(props.queues().clientCreated(), true);
    }

    @Bean
    public Queue clientStatusQueue(ClientRabbitProperties props) {
        return new Queue(props.queues().clientStatus(), true);
    }

    @Bean
    public Queue clientUpdatedQueue(ClientRabbitProperties props){
        return new Queue(props.queues().clientUpdated(), true);
    }

    @Bean
    public Binding bindingClientCreated(Queue clientCreatedQueue, DirectExchange clientDirectExchange, ClientRabbitProperties props) {
        return BindingBuilder.bind(clientCreatedQueue).to(clientDirectExchange).with(props.routingKeys().created());
    }

    @Bean
    public Binding bindingClientStatus(Queue clientStatusQueue, DirectExchange clientDirectExchange, ClientRabbitProperties props) {
        return BindingBuilder.bind(clientStatusQueue).to(clientDirectExchange).with(props.routingKeys().statusChanged());
    }

    @Bean
    public Binding bindingClientUpdated(Queue clientUpdatedQueue, DirectExchange clientDirectExchange, ClientRabbitProperties props) {
        return BindingBuilder.bind(clientUpdatedQueue).to(clientDirectExchange).with(props.routingKeys().updated());
    }

}
