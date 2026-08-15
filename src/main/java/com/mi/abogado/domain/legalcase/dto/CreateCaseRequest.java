package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.CasePriority;
import com.mi.abogado.domain.legalcase.entity.CaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * El {@code caseNumber} no viaja aqui: lo asigna el sistema (consecutivo de la
 * firma). Dejarlo en manos del usuario garantizaria duplicados.
 */
public record CreateCaseRequest(
        @NotNull UUID clientId,
        @NotBlank @Size(max = 200) String title,
        @NotNull CaseType caseType,
        @Size(max = 4000) String description,
        UUID assignedLawyerId,
        UUID practiceAreaId,
        CasePriority priority,
        @Size(max = 30) String radicado,
        @Size(max = 180) String court,
        @Size(max = 180) String opposingParty,
        @PositiveOrZero BigDecimal claimAmount
) {
}
