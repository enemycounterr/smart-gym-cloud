package com.sprint.smartgymcore.mapper;


import com.sprint.smartgymcore.dto.access.AccessCheckRequest;
import com.sprint.smartgymcore.dto.access.AccessLogResponse;
import com.sprint.smartgymcore.model.AccessLog;
import com.sprint.smartgymcore.model.AccessZone;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AccessMapper {

    public AccessLogResponse toDto(AccessLog log, String clientName) {
        if (log == null) return null;

        return new AccessLogResponse(
                log.getId(),
                log.getClientId(),
                clientName,
                log.getAccessZone().getZoneName(),
                log.getDirection(),
                log.getTimeStamp()
        );
    }

    public AccessLog toEntity(AccessCheckRequest request, Long clientId, AccessZone accessZone) {
        if (request == null || clientId == null || accessZone == null) {
            return null;
        }

        return AccessLog.builder()
                .direction(request.direction())
                .clientId(clientId)
                .accessZone(accessZone)
                .timeStamp(Instant.now())
                .build();
    }
}
