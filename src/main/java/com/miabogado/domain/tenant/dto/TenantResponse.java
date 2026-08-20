package com.miabogado.domain.tenant.dto;

import com.miabogado.domain.tenant.entity.TenantStatus;

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
