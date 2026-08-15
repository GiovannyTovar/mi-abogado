package com.mi.abogado.domain.lawyer.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Proyeccion para el listado. La consulta la construye directamente con estos
 * campos: no se cargan entidades ni colecciones, asi que no hay N+1 posible.
 */
public record LawyerSummary(
        UUID id,
        String fullName,
        String email,
        String photoUrl,
        String licenseNumber,
        String city,
        short yearsOfExperience,
        boolean published,
        BigDecimal ratingAvg
) {
}
