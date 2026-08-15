package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.CaseOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CloseCaseRequest(
        @NotNull CaseOutcome outcome,
        @Size(max = 4000) String closingNote
) {
}
