package com.miabogado.domain.client.dto;

import com.miabogado.domain.client.entity.ClientStatus;
import com.miabogado.domain.client.entity.ClientType;
import com.miabogado.domain.client.entity.DocumentType;

import java.util.UUID;

/**
 * Fila del CRM. Incluye el numero de casos abiertos porque es lo primero que
 * mira el abogado al abrir la lista, y traerlo en la misma consulta evita una
 * llamada por fila.
 */
public record ClientSummary(
        UUID id,
        ClientType clientType,
        DocumentType documentType,
        String documentNumber,
        String name,
        String email,
        String phone,
        String city,
        ClientStatus status,
        long openCases
) {
}
