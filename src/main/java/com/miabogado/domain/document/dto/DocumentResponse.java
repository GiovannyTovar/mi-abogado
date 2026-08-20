package com.miabogado.domain.document.dto;

import com.miabogado.domain.document.entity.DocumentSource;
import com.miabogado.domain.document.entity.DocumentVisibility;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code storageKey} no aparece: es una ruta interna del servidor y el cliente
 * no tiene por que conocerla. La descarga va siempre por el id.
 */
public record DocumentResponse(
        UUID id,
        UUID caseId,
        String name,
        String contentType,
        long sizeBytes,
        DocumentVisibility visibility,
        DocumentSource source,
        String description,
        String uploadedByName,
        Instant createdAt
) {
}
