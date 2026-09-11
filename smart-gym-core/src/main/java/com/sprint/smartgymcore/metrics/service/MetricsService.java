package com.sprint.smartgymcore.metrics.service;


import com.sprint.smartgymcore.model.AccessDirection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private final Counter crmSuccessCounter;
    private final Timer crmCallDurationTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.crmSuccessCounter = Counter.builder("crm.call.success")
                .description("Number of successful calls to CRM")
                .register(meterRegistry);

        this.crmCallDurationTimer = Timer.builder("crm.call.duration")
                .description("Time taken for CRM calls")
                .register(meterRegistry);
    }

    public void incrementAccessEventReceived(AccessDirection direction) {
        meterRegistry.counter("access.event.received", "direction", direction.name()).increment();
    }

    public void incrementCrmSuccess() {
        crmSuccessCounter.increment();
    }

    public void incrementCrmError(String errorCategory) {
        meterRegistry.counter("crm.call.error", "error_type", errorCategory).increment();
    }

    public void recordCrmCallDuration(long durationMs) {
        crmCallDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
