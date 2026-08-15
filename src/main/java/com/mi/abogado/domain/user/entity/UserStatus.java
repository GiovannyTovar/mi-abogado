package com.mi.abogado.domain.user.entity;

public enum UserStatus {
    /** Invitado: existe la fila pero aun no ha entrado con Google (firebaseUid nulo). */
    PENDING,
    ACTIVE,
    DISABLED
}
