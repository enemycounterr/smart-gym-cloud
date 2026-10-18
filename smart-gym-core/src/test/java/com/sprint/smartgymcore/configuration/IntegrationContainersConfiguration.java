package com.sprint.smartgymcore.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationContainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofSeconds(60));
    }

//    @Bean
//    @ServiceConnection(name = "redis")
//    RedisContainer redis() {
//        return new RedisContainer(DockerImageName.parse("redis:7-alpine"));
//    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitmq() {
        return new RabbitMQContainer("rabbitmq:3-management-alpine");
    }
}