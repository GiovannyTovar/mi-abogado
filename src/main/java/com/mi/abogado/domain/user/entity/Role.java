package com.mi.abogado.domain.user.entity;

public enum Role {
    /** Plataforma: gestiona firmas y planes. No pertenece a ninguna firma. */
    SUPER_ADMIN,
    /** Dueno de la firma: factura, invita miembros, ve todo el tenant. */
    FIRM_OWNER,
    LAWYER,
    /** Asistente: apoya al abogado, sin acceso a facturacion. */
    ASSISTANT,
    /** Cliente final: solo ve sus propios casos en el portal white-label. */
    CLIENT
}
