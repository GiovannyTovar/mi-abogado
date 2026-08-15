package com.mi.abogado.domain.portal.dto;

import com.mi.abogado.domain.document.dto.DocumentResponse;
import com.mi.abogado.domain.legalcase.dto.CaseEventResponse;
import com.mi.abogado.domain.legalcase.entity.CaseOutcome;
import com.mi.abogado.domain.legalcase.entity.CaseStatus;
import com.mi.abogado.domain.legalcase.entity.CaseType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Estado del caso tal como lo ve su dueno: que pasa, quien lo lleva, que
 * documentos hay compartidos y que actuaciones se publicaron.
 * <p>
 * No incluye cuantia, contraparte, juzgado ni prioridad interna: son datos de
 * trabajo de la firma, y algunos (la cuantia) condicionan la conversacion sobre
 * honorarios.
 */
public record PortalCaseDetail(
        UUID id,
        String caseNumber,
        String title,
        String description,
        CaseType caseType,
        CaseStatus status,
        CaseOutcome outcome,
        String lawyerName,
        String practiceAreaName,
        Instant openedAt,
        Instant closedAt,
        List<CaseEventResponse> timeline,
        List<DocumentResponse> documents
) {
}
