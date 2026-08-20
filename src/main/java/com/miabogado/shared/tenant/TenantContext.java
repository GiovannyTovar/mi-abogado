package com.miabogado.shared.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant de la peticion en curso. Lo escribe {@code JwtAuthenticationFilter} al
 * validar el token y lo limpia al terminar la peticion.
 */
public final class TenantContext {

    /**
     * Tenant "imposible" que se usa cuando la peticion no tiene firma asociada
     * (super-admin o endpoint publico). Ninguna fila lo lleva, asi que el filtro
     * de Hibernate devuelve vacio en vez de devolver datos de otra firma: falla cerrado.
     */
    public static final UUID NO_TENANT = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> find() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * @throws IllegalStateException si se invoca fuera de una peticion con firma.
     *         Es un error de programacion, no una entrada invalida del usuario.
     */
    public static UUID require() {
        UUID tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No hay tenant en el contexto de la peticion");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
