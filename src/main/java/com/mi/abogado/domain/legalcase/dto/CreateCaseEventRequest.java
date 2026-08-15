package com.mi.abogado.domain.legalcase.dto;

import com.mi.abogado.domain.legalcase.entity.CaseEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * @param occurredAt cuando ocurrio de verdad, que no siempre es cuando se
 *                   registra. Si viene null, se usa el momento actual.
 */
public record CreateCaseEventRequest(
        @NotNull CaseEventType eventType,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        Instant occurredAt
) {
}
