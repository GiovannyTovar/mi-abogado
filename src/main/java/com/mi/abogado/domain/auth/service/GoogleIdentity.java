package com.mi.abogado.domain.auth.service;

/**
 * Datos de la persona tal como los certifica Firebase tras validar el ID token.
 * Nada de esto se cree por si solo: sirve para localizar al {@code User} local,
 * que es quien tiene rol y tenant.
 */
public record GoogleIdentity(
        String firebaseUid,
        String email,
        boolean emailVerified,
        String fullName,
        String photoUrl
) {
}
