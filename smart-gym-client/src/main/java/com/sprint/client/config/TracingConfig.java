package com.sprint.client.config;


import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public ObservationPredicate ignoreScheduledTasks() {
        /*
         Ignore observations related to background task execution @Scheduled
         */
        return (name, context) -> !name.startsWith("tasks.scheduled");
    }
}