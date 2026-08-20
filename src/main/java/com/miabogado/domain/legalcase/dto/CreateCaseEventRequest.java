package com.miabogado.domain.legalcase.dto;

import com.miabogado.domain.legalcase.entity.CaseEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * @param occurredAt      cuando ocurrio de verdad, que no siempre es cuando se
 *                        registra. Si viene null, se usa el momento actual.
 * @param visibleToClient si se publica en el portal del cliente. Por defecto no.
 */
public record CreateCaseEventRequest(
        @NotNull CaseEventType eventType,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        Instant occurredAt,
        Boolean visibleToClient
) {
}
