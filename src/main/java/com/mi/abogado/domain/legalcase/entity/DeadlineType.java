package com.mi.abogado.domain.legalcase.entity;

public enum DeadlineType {
    /** Termino procesal: contestar, apelar, subsanar. Perderlo es perder el caso. */
    TERMINO_LEGAL,
    AUDIENCIA,
    /** Vencimiento contractual o de prescripcion. */
    VENCIMIENTO,
    RECORDATORIO
}
