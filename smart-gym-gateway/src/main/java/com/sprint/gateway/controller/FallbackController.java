package com.sprint.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback(){
        return buildFallbackResponse("Auth Service is currently unavailable. Please try again later.");
    }

    @RequestMapping("/core")
    public Mono<ResponseEntity<Map<String, Object>>> coreFallback() {
        return buildFallbackResponse("Access Control Service is temporarily unavailable.");
    }

    @RequestMapping("/client")
    public Mono<ResponseEntity<Map<String, Object>>> clientFallback() {
        return buildFallbackResponse("Client Management Service is down or experiencing heavy load.");
    }

    @RequestMapping("/notification")
    public Mono<ResponseEntity<Map<String, Object>>> notificationFallback() {
        return buildFallbackResponse("Notification Service is currently unreachable.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(String message) {
        Map<String, Object> response = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", message
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
