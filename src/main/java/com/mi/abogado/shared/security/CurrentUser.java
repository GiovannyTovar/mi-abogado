package com.mi.abogado.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Acceso al principal autenticado sin inyectar {@code Authentication} en cada firma.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthPrincipal require() {
        return find().orElseThrow(() -> new IllegalStateException("No hay usuario autenticado en el contexto"));
    }

    /**
     * Variante tolerante: vacio cuando el codigo corre fuera de una peticion,
     * como en los jobs programados.
     */
    public static Optional<AuthPrincipal> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal
                ? Optional.of(principal)
                : Optional.empty();
    }
}
