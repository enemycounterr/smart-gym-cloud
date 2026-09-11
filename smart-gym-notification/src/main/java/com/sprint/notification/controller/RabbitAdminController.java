package com.sprint.notification.controller;

import com.sprint.notification.service.RabbitAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/rabbitmq")
@RequiredArgsConstructor
public class RabbitAdminController {
    private final RabbitAdminService rabbitAdminService;

    @PostMapping("/reprocess-dlq")
    public ResponseEntity<Map<String, Object>> reprocessDlq() {
        int count = rabbitAdminService.reprocessDlqMessages();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Messages successfully re-queued for processing",
                "count", count
        ));
    }
}
