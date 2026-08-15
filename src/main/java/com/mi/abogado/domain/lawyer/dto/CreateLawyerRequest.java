package com.mi.abogado.domain.lawyer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Alta de abogado: crea la invitacion del usuario y su perfil en una sola operacion.
 * La persona entra despues con Google y su cuenta queda vinculada.
 */
public record CreateLawyerRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 40) String licenseNumber,
        @Size(max = 4000) String bio,
        @PositiveOrZero @Max(70) Short yearsOfExperience,
        @Size(max = 100) String city,
        @PositiveOrZero BigDecimal hourlyRate,
        Set<UUID> practiceAreaIds
) {
}
