package com.sprint.smartgymcore.validator.impl;

import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.exceptions.ZoneAccessDeniedException;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.model.AccessLog;
import com.sprint.smartgymcore.model.AccessZone;
import com.sprint.smartgymcore.repository.AccessLogRepository;
import com.sprint.smartgymcore.validator.AccessRuleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(3)
@RequiredArgsConstructor

public class AntiPassbackValidator implements AccessRuleValidator {

    private final AccessLogRepository accessLogRepository;

    @Override
    public void validate(AccessCard card, AccessZone accessZone, AccessCheckRequest request) {
        Optional<AccessLog> latestLog = this.accessLogRepository.
                findFirstByClientIdOrderByTimeStampDesc(card.getClientId());

        if (latestLog.isPresent() && latestLog.get().getDirection() == request.direction()) {
            throw new ZoneAccessDeniedException("Anti-Passback violation! Client '" + card.getClientName() +
                    "' already performed direction: " + request.direction());
        }

    }
}