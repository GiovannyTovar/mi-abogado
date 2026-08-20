package com.miabogado.shared.security;

import com.miabogado.domain.user.entity.Role;

import java.util.UUID;

/**
 * Identidad autenticada de la peticion, reconstruida desde el JWT propio.
 * No toca la BD: todo lo que hace falta para autorizar viaja en el token.
 *
 * @param tenantId null para el super-admin, que no pertenece a ninguna firma.
 */
public record AuthPrincipal(UUID userId, UUID tenantId, Role role, String email) {

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }
}
