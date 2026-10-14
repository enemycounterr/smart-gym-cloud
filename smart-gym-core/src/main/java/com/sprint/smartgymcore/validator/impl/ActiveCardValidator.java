package com.sprint.smartgymcore.validator.impl;

import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.exceptions.ZoneAccessDeniedException;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.model.AccessZone;
import com.sprint.smartgymcore.validator.AccessRuleValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ActiveCardValidator implements AccessRuleValidator {
    @Override
    public void validate(AccessCard card, AccessZone accessZone, AccessCheckRequest request) {
        if (!card.isActive()) {
            throw new ZoneAccessDeniedException("Access denied! Card is inactive!");
        }
    }
}
