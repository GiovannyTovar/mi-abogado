package com.miabogado.domain.legalcase.entity;

public enum CaseStatus {
    /** Recien abierto, sin actuaciones todavia. */
    OPEN,
    IN_PROGRESS,
    /** Detenido por causa externa: espera de audiencia, de la contraparte, del juzgado. */
    ON_HOLD,
    CLOSED
}
