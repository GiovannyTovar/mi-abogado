package com.mi.abogado.domain.message.dto;

import com.mi.abogado.domain.user.entity.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * @param senderRole permite al cliente pintar el mensaje a un lado u otro sin
 *                   conocer los ids de usuario de la firma.
 */
public record MessageResponse(
        UUID id,
        String body,
        UUID senderId,
        String senderName,
        Role senderRole,
        Instant readAt,
        Instant createdAt
) {
}
