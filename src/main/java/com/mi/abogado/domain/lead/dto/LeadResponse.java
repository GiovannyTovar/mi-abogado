package com.mi.abogado.domain.lead.dto;

import com.mi.abogado.domain.lead.entity.LeadSource;
import com.mi.abogado.domain.lead.entity.LeadStatus;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String city,
        LeadSource source,
        String message,
        LeadStatus status,
        String lostReason,
        UUID practiceAreaId,
        String practiceAreaName,
        UUID assignedLawyerId,
        String assignedLawyerName,
        UUID convertedClientId,
        Instant contactedAt,
        Instant createdAt
) {
}
