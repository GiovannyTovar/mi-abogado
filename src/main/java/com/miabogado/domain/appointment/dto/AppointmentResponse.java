package com.miabogado.domain.appointment.dto;

import com.miabogado.domain.appointment.entity.AppointmentMode;
import com.miabogado.domain.appointment.entity.AppointmentStatus;

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
