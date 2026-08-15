package com.mi.abogado.domain.lead.entity;

/** Etapas del pipeline, en orden. */
public enum LeadStatus {
    /** Entro y nadie lo ha tocado. */
    NEW,
    CONTACTED,
    /** Tiene caso viable y quiere avanzar. */
    QUALIFIED,
    /** Se volvio cliente de la firma. */
    CONVERTED,
    LOST
}
