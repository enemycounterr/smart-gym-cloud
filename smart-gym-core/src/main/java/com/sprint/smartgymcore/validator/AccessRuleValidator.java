package com.sprint.smartgymcore.validator;

import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.model.AccessZone;


public interface AccessRuleValidator {
    /**
     * Validates an attempt to pass through the turnstile.
     * If a rule is violated, the method must throw a ZoneAccessDeniedException.
     */
    void validate(AccessCard card, AccessZone accessZone, AccessCheckRequest request);
}
