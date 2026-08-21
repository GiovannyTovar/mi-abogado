package com.miabogado.domain.settlement.entity;

/**
 * Motivo de la terminacion. Solo {@link #SIN_JUSTA_CAUSA} causa indemnizacion;
 * las prestaciones se deben siempre, incluso si el trabajador renuncio.
 */
public enum TerminationReason {
    SIN_JUSTA_CAUSA,
    JUSTA_CAUSA,
    RENUNCIA,
    MUTUO_ACUERDO,
    /** Se cumplio el plazo del contrato a termino fijo con preaviso en regla. */
    VENCIMIENTO_PLAZO,
    /** Termino la obra para la que se contrato. */
    OBRA_TERMINADA
}
