package com.mi.abogado.domain.tenant.dto;

import com.mi.abogado.domain.tenant.entity.TenantStatus;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        String nit,
        String contactEmail,
        String contactPhone,
        TenantStatus status,
        Instant createdAt
) {
}
