package com.miabogado.domain.legalcase.dto;

import com.miabogado.domain.legalcase.entity.CaseOutcome;
import com.miabogado.domain.legalcase.entity.CasePriority;
import com.miabogado.domain.legalcase.entity.CaseStatus;
import com.miabogado.domain.legalcase.entity.CaseType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Ficha completa del expediente. */
public record CaseResponse(
        UUID id,
        String caseNumber,
        String radicado,
        String title,
        String description,
        CaseType caseType,
        CaseStatus status,
        CaseOutcome outcome,
        CasePriority priority,
        UUID clientId,
        String clientName,
        UUID assignedLawyerId,
        String assignedLawyerName,
        UUID practiceAreaId,
        String practiceAreaName,
        String court,
        String opposingParty,
        BigDecimal claimAmount,
        Instant openedAt,
        Instant closedAt
) {
}
