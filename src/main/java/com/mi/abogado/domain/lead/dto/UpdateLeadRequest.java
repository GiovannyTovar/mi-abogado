package com.mi.abogado.domain.lead.dto;

import com.mi.abogado.domain.lead.entity.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Edicion parcial. {@code status} no admite CONVERTED: convertir crea un cliente
 * y va por {@code POST /leads/&#123;id&#125;/convert}.
 */
public record UpdateLeadRequest(
        @Size(max = 180) String name,
        @Email @Size(max = 180) String email,
        @Size(max = 30) String phone,
        @Size(max = 100) String city,
        @Size(max = 4000) String message,
        LeadStatus status,
        @Size(max = 250) String lostReason,
        UUID practiceAreaId,
        UUID assignedLawyerId
) {
}
