package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.CasePriority;
import com.mi.abogado.domain.legalcase.entity.CaseStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Edicion parcial. {@code status} no admite CLOSED: cerrar exige desenlace y va
 * por {@code POST /cases/&#123;id&#125;/close}.
 */
public record UpdateCaseRequest(
        @Size(max = 200) String title,
        @Size(max = 4000) String description,
        CaseStatus status,
        CasePriority priority,
        UUID assignedLawyerId,
        UUID practiceAreaId,
        @Size(max = 30) String radicado,
        @Size(max = 180) String court,
        @Size(max = 180) String opposingParty,
        @PositiveOrZero BigDecimal claimAmount
) {
}
