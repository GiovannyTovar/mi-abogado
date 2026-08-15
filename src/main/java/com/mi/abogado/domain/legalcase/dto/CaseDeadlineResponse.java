package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.DeadlineStatus;
import com.mi.abogado.domain.legalcase.entity.DeadlineType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CaseDeadlineResponse(
        UUID id,
        DeadlineType deadlineType,
        String title,
        String description,
        LocalDate dueDate,
        short notifyDaysBefore,
        DeadlineStatus status,
        Instant completedAt
) {
}
