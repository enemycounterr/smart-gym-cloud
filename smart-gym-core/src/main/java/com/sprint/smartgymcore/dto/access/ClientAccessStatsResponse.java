package com.sprint.smartgymcore.dto.access;

public record ClientAccessStatsResponse(Long clientId, String clientName, long totalEntries, long totalExits) {
}
