package com.miabogado.domain.client.dto;

import com.miabogado.domain.client.entity.ClientStatus;
import com.miabogado.domain.client.entity.ClientType;
import com.miabogado.domain.client.entity.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        ClientType clientType,
        DocumentType documentType,
        String documentNumber,
        String name,
        String email,
        String phone,
        String address,
        String city,
        String notes,
        ClientStatus status,
        Instant createdAt
) {
}
