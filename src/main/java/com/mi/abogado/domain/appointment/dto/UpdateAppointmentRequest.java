package com.mi.abogado.domain.appointment.dto;

import com.mi.abogado.domain.appointment.entity.AppointmentMode;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Reprogramar o corregir. El estado no viaja aqui: confirmar, cancelar y
 * completar son acciones con su propio endpoint y sus propias reglas.
 */
public record UpdateAppointmentRequest(
        @Size(max = 200) String title,
        @Size(max = 4000) String description,
        AppointmentMode mode,
        @Size(max = 250) String location,
        Instant startsAt,
        Instant endsAt,
        UUID lawyerId
) {
}
