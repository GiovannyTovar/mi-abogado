package com.miabogado.domain.legalcase.dto;

import com.miabogado.domain.legalcase.entity.CasePriority;
import com.miabogado.domain.legalcase.entity.CaseStatus;
import com.miabogado.domain.legalcase.entity.CaseType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila del listado de expedientes.
 *
 * @param nextDueDate proximo termino pendiente; es la columna que de verdad
 *                    importa en el dia a dia, asi que se resuelve en la misma
 *                    consulta y no con una llamada por fila.
 */
public record CaseSummary(
        UUID id,
        String caseNumber,
        String title,
        CaseType caseType,
        CaseStatus status,
        CasePriority priority,
        String clientName,
        String assignedLawyerName,
        LocalDate nextDueDate,
        Instant openedAt
) {
}
