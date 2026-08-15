package com.mi.abogado.domain.portal.dto;

import com.mi.abogado.domain.legalcase.entity.CaseStatus;
import com.mi.abogado.domain.legalcase.entity.CaseType;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que el cliente ve de su caso en la lista. Deliberadamente mas corto que
 * {@code CaseSummary}: sin prioridad interna, sin cuantia, sin contraparte.
 */
public record PortalCaseSummary(
        UUID id,
        String caseNumber,
        String title,
        CaseType caseType,
        CaseStatus status,
        String lawyerName,
        Instant openedAt,
        long unreadMessages
) {
}
