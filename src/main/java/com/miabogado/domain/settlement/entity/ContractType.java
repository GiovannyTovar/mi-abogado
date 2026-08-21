package com.miabogado.domain.settlement.entity;

/**
 * Modalidad del contrato. Determina como se calcula la indemnizacion del
 * art. 64 CST, que es lo unico que cambia entre modalidades: las prestaciones
 * (cesantias, prima, vacaciones) se liquidan igual en las tres.
 */
public enum ContractType {
    /** Termino indefinido: 30+20 dias, o 20+15 si el salario supera el umbral. */
    INDEFINIDO,
    /** Termino fijo: los salarios que faltaban hasta la fecha pactada. */
    FIJO,
    /** Obra o labor: el tiempo que faltaba para terminar la obra. */
    OBRA_LABOR
}
