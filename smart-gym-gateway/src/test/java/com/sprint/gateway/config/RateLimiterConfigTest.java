package com.sprint.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterConfigTest {
    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    @DisplayName("userKeyResolver: should return token when Bearer header is present")
    void userKeyResolver_extractsBearerToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/clients")
                .header("Authorization", "Bearer my-secret-jwt-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(config.userKeyResolver().resolve(exchange))
                .expectNext("my-secret-jwt-token")
                .verifyComplete();
    }

    @Test
    @DisplayName("userKeyResolver: should fallback to IP when Authorization header is missing")
    void userKeyResolver_fallbacksToIpWhenHeaderMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/clients")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(config.userKeyResolver().resolve(exchange))
                .expectNext("192.168.1.100")
                .verifyComplete();
    }

    @Test
    @DisplayName("ipKeyResolver: should resolve remote IP address")
    void ipKeyResolver_resolvesIp() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(config.ipKeyResolver().resolve(exchange))
                .expectNext("10.0.0.1")
                .verifyComplete();
    }
}