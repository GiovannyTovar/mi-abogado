package com.mi.abogado.domain.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Alta de bufete: la firma y su dueno nacen juntos. Una firma sin dueno no
 * tendria a nadie que pudiera entrar a configurarla.
 */
public record CreateTenantRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 30) String nit,
        @Size(max = 30) String contactPhone,
        @NotNull UUID planId,
        @NotBlank @Email @Size(max = 180) String ownerEmail,
        @NotBlank @Size(max = 150) String ownerFullName
) {
}
