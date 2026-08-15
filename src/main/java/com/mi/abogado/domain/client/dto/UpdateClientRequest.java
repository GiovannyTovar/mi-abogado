package com.mi.abogado.domain.client.dto;

import com.mi.abogado.domain.client.entity.ClientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Edicion parcial: null = no tocar. El documento no se edita — si esta mal,
 * es otra persona, y mezclar dos identidades en la misma ficha corrompe el CRM.
 */
public record UpdateClientRequest(
        @Size(max = 180) String name,
        @Email @Size(max = 180) String email,
        @Size(max = 30) String phone,
        @Size(max = 250) String address,
        @Size(max = 100) String city,
        @Size(max = 4000) String notes,
        ClientStatus status
) {
}
