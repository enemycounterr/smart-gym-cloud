package com.sprint.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    /**
     * Client identification by IP address.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(resolveIp(exchange));

    }

    /**
     * Client identification by JWT token from Authorization header, falls back to IP address if token is missing
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                return Mono.just(authHeader.substring(7));

            }

            return Mono.just(resolveIp(exchange));
        };
    }

    private String resolveIp(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("127.0.0.1");
    }
}
