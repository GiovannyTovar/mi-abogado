package com.miabogado.domain.legalcase.dto;

import com.miabogado.domain.legalcase.entity.DeadlineStatus;
import com.miabogado.domain.legalcase.entity.DeadlineType;

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
