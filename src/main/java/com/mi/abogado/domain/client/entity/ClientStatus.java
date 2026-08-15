package com.mi.abogado.domain.client.entity;

public enum ClientStatus {
    ACTIVE,
    /** Archivado: ya no trabaja con la firma. No se borra, su historial de casos sigue. */
    INACTIVE
}
