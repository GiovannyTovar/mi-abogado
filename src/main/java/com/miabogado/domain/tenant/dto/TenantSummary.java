package com.miabogado.domain.tenant.dto;

import com.miabogado.domain.tenant.entity.TenantStatus;

import java.time.Instant;
import java.util.UUID;

/** Fila del listado de firmas del super-admin. Proyeccion: no carga entidades. */
public record TenantSummary(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        String planName,
        Instant createdAt
) {
}
