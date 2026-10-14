package com.sprint.smartgymcore.validator.impl;


import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.exceptions.ZoneAccessDeniedException;
import com.sprint.smartgymcore.model.AccessCard;
import com.sprint.smartgymcore.model.AccessZone;
import com.sprint.smartgymcore.repository.AccessZoneRepository;
import com.sprint.smartgymcore.validator.AccessRuleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class ZonePermissionValidator implements AccessRuleValidator {

    private final AccessZoneRepository zoneRepository;

    @Override
    public void validate(AccessCard card, AccessZone accessZone, AccessCheckRequest request) {
        if (!this.zoneRepository.hasClientAccess(request.zoneId(), card.getClientId())) {
            throw new ZoneAccessDeniedException("Access denied! Client '" + card.getClientName() +
                    "' does not have permission for zone '" + accessZone.getZoneName() + "'");
        }
    }
}
