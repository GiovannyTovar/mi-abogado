package com.mi.abogado.domain.appointment.dto;

import com.mi.abogado.domain.appointment.entity.AppointmentMode;
import com.mi.abogado.domain.appointment.entity.AppointmentStatus;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        String title,
        String description,
        AppointmentMode mode,
        String location,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        String cancelReason,
        UUID clientId,
        String clientName,
        UUID caseId,
        String caseNumber,
        UUID lawyerId,
        String lawyerName
) {
}
