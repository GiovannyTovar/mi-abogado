package com.mi.abogado.domain.appointment.entity;

public enum AppointmentMode {
    PRESENCIAL,
    /** Videollamada: {@code location} lleva el enlace. */
    VIRTUAL,
    TELEFONICA
}
