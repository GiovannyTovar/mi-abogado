package com.miabogado.domain.settlement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Proyeccion del listado: no materializa la entidad ni sus relaciones. */
public record SettlementSummary(UUID id,
                                String employeeName,
                                UUID clientId,
                                String clientName,
                                UUID legalCaseId,
                                String caseNumber,
                                LocalDate endDate,
                                BigDecimal total,
                                Instant createdAt) {
}
