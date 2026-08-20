package com.miabogado.domain.legalcase.entity;

/** Desenlace del expediente. Obligatorio al cerrar, nulo mientras esta abierto. */
public enum CaseOutcome {
    WON,
    LOST,
    /** Conciliado: acuerdo entre las partes, muy frecuente en laboral. */
    SETTLED,
    /** Desistido o retirado por el cliente. */
    WITHDRAWN
}
