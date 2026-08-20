package com.miabogado.domain.legalcase.dto;

import com.miabogado.domain.legalcase.entity.CaseOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CloseCaseRequest(
        @NotNull CaseOutcome outcome,
        @Size(max = 4000) String closingNote
) {
}
