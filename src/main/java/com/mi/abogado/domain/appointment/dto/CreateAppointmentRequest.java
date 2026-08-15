package com.mi.abogado.domain.appointment.dto;

import com.mi.abogado.domain.appointment.entity.AppointmentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID clientId,
        UUID caseId,
        UUID lawyerId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull AppointmentMode mode,
        @Size(max = 250) String location,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt
) {
}
