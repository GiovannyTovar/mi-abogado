package com.miabogado.domain.settlement.dto;

/** Los renglones de una liquidacion laboral colombiana. */
public enum SettlementConcept {
    CESANTIAS,
    INTERESES_CESANTIAS,
    PRIMA_SERVICIOS,
    VACACIONES,
    /** Art. 64 CST. Solo cuando el despido fue sin justa causa. */
    INDEMNIZACION
}
