package com.miabogado.domain.user.dto;

import com.miabogado.domain.user.entity.Role;
import com.miabogado.domain.user.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** Fila del listado de miembros del equipo. Proyeccion, no entidad. */
public record MemberSummary(
        UUID id,
        String fullName,
        String email,
        String photoUrl,
        Role role,
        UserStatus status,
        Instant lastLoginAt
) {
}
