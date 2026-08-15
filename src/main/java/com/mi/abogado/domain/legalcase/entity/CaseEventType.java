package com.mi.abogado.domain.legalcase.entity;

public enum CaseEventType {
    NOTA,
    /** Actuacion procesal: se presento la demanda, el juzgado admitio, etc. */
    ACTUACION,
    AUDIENCIA,
    DOCUMENTO,
    /** Lo registra el sistema cuando cambia el estado del expediente. */
    CAMBIO_ESTADO,
    /** Contacto con el cliente o la contraparte. */
    COMUNICACION
}
