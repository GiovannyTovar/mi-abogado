package com.mi.abogado.domain.lawyer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Edicion parcial del perfil: los campos nulos se dejan como estan.
 * El rating no aparece: lo calcula el sistema, no el usuario.
 */
public record UpdateLawyerRequest(
        @Size(max = 40) String licenseNumber,
        @Size(max = 4000) String bio,
        @PositiveOrZero @Max(70) Short yearsOfExperience,
        @Size(max = 100) String city,
        @PositiveOrZero BigDecimal hourlyRate,
        Set<UUID> practiceAreaIds
) {
}
