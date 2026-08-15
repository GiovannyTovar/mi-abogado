package com.mi.abogado.domain.lawyer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ficha completa del abogado (detalle). */
public record LawyerResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        String phone,
        String photoUrl,
        String licenseNumber,
        String bio,
        short yearsOfExperience,
        String city,
        String publicSlug,
        boolean published,
        BigDecimal hourlyRate,
        BigDecimal ratingAvg,
        int ratingCount,
        List<PracticeAreaResponse> practiceAreas,
        Instant createdAt
) {
}
