package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.DeadlineType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila de la agenda de vencimientos de toda la firma. Trae el contexto del
 * expediente para que la pantalla no tenga que pedirlo caso por caso.
 */
public record UpcomingDeadline(
        UUID id,
        UUID caseId,
        String caseNumber,
        String caseTitle,
        String clientName,
        String assignedLawyerName,
        DeadlineType deadlineType,
        String title,
        LocalDate dueDate,
        short notifyDaysBefore
) {
}
