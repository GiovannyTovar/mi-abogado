package com.miabogado.domain.lead.dto;

import com.miabogado.domain.lead.entity.LeadSource;
import com.miabogado.domain.lead.entity.LeadStatus;

import java.time.Instant;
import java.util.UUID;

/** Tarjeta del pipeline. */
public record LeadSummary(
        UUID id,
        String name,
        String email,
        String phone,
        String city,
        LeadSource source,
        LeadStatus status,
        String practiceAreaName,
        String assignedLawyerName,
        Instant createdAt
) {
}
