package com.sprint.smartgymcore.mapper;

import com.sprint.smartgymcore.dto.zone.AccessZoneCreateRequest;
import com.sprint.smartgymcore.dto.zone.AccessZoneResponse;
import com.sprint.smartgymcore.model.AccessZone;
import org.springframework.stereotype.Component;

@Component
public class AccessZoneMapper {
    public AccessZoneResponse toDto(AccessZone zone) {
        if (zone == null) return null;
        return new AccessZoneResponse(zone.getId(), zone.getZoneName());
    }

    public AccessZone toEntity(AccessZoneCreateRequest request) {
        if (request == null) return null;
        AccessZone zone = new AccessZone();
        zone.setZoneName(request.zoneName().toUpperCase());
        return zone;
    }

}
