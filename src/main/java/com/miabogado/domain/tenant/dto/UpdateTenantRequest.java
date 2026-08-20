package com.miabogado.domain.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Datos que la firma puede editar de si misma. El {@code slug} no esta: ya circula
 * en enlaces del portal del cliente y cambiarlo los rompe.
 */
public record UpdateTenantRequest(
        @Size(max = 150) String name,
        @Size(max = 30) String nit,
        @Email @Size(max = 180) String contactEmail,
        @Size(max = 30) String contactPhone
) {
}
