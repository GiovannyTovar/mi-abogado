package com.mi.abogado.domain.lead.dto;

import com.mi.abogado.domain.lead.entity.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Alta manual de un lead. El marketplace (Fase 8) y la calculadora publica
 * (Fase 5) crearan leads por su propia via, sin sesion.
 * <p>
 * Correo o telefono: al menos uno hace falta, o no hay a quien contactar. Lo
 * valida el service y lo respalda la restriccion ck_lead_contact.
 */
public record CreateLeadRequest(
        @NotBlank @Size(max = 180) String name,
        @Email @Size(max = 180) String email,
        @Size(max = 30) String phone,
        @Size(max = 100) String city,
        @NotNull LeadSource source,
        @Size(max = 4000) String message,
        UUID practiceAreaId,
        UUID assignedLawyerId
) {
}
