package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.CaseEventType;

import java.time.Instant;
import java.util.UUID;

public record CaseEventResponse(
        UUID id,
        CaseEventType eventType,
        String title,
        String description,
        String createdByName,
        boolean visibleToClient,
        Instant occurredAt
) {
}
