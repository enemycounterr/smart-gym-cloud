package com.sprint.smartgymcore.messaging.outbox.scheduler;

import com.sprint.smartgymcore.messaging.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private final OutboxService outboxService;

    @Value("${app.outbox.cleanup-scheduler.retention-days:7}")
    private int retentionDays;

    @Value("${app.outbox.cleanup-scheduler.batch-size:1000}")
    private int batchSize;

    @Scheduled(cron = "${app.outbox.cleanup-scheduler.cron:0 0 3 * * *}")
    public void cleanUpOldSentEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        log.info("Starting outbox cleanup for SENT events older than {} days (cutoff: {})", retentionDays, cutoff);

        int totalDeleted = 0;
        while (true) {
            int deletedInBatch = outboxService.deleteSentEventsBatch(cutoff, this.batchSize);
            totalDeleted += deletedInBatch;
            if (deletedInBatch < this.batchSize) {
                break;
            }
        }

        if (totalDeleted > 0) {
            log.info("Outbox cleanup finished. Total deleted: {} events", totalDeleted);
        }
    }
}
