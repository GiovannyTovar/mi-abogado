package com.mi.abogado.domain.appointment.entity;

public enum AppointmentStatus {
    /** Agendada por la firma, sin confirmar por el cliente. */
    SCHEDULED,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    /** El cliente no se presento. Vale la pena distinguirlo de una cancelacion. */
    NO_SHOW
}
